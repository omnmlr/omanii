package com.omanii.app.pose

enum class PoseDataOrigin { LIVE_ARCORE, SYNTHETIC_FIXTURE }
data class PoseReplay(
    val dataOrigin: PoseDataOrigin,
    val records: List<PoseRecord>,
    val canonicalRecords: List<CanonicalPoseRecord>? = null,
)

/** Task-local debug dialect, not a new shared protocol version. No storage or upload side effects. */
object PoseJsonl {
    const val SCHEMA = "pose-debug-v1"
    const val PROTOCOL = "pose-capture-v1"
    private const val COORDINATES = "RIGHT_HANDED_Y_UP_METRES_XZ"
    private val fields = setOf(
        "schema_version", "protocol_version", "test_profile", "data_origin", "source", "coordinates", "orientation",
        "session_id", "segment_id", "coordinate_frame_id", "received_at_monotonic_ns", "measurement_at_monotonic_ns",
        "age_known", "kind", "tracking", "source_timestamp_ns", "source_clock_domain", "source_event_meaning",
        "x_m", "y_m", "z_m", "qx", "qy", "qz", "qw", "reason", "tracking_failure",
        "previous_segment_id", "previous_coordinate_frame_id",
    )
    private val canonicalFields = setOf(
        "canonical_record_id", "canonical_producer", "canonical_app_build", "canonical_run_token",
        "canonical_profile_availability", "canonical_profile_reason", "canonical_source_clock_instance_token",
        "canonical_source_time_meaning", "canonical_segment_session_id", "canonical_coordinate_frame_session_id",
        "canonical_spatial_association_availability", "canonical_pose_observation_id",
    )

    /** Retains the v1 debug fields/provenance and adds the approved shared development projection. */
    fun encodeCanonical(records: List<CanonicalPoseRecord>): String {
        require(records.isNotEmpty())
        val context = records.first().mappingContext
        require(records.all { it.mappingContext == context })
        require(records.map { it.metadata.recordId }.distinct().size == records.size)
        val local = records.map { it.debugRecord }
        val lines = encode(local, context.debug.dataOrigin).lineSequence().filter { it.isNotBlank() }.toList()
        val mapper = PoseCanonicalMapper(context)
        return lines.mapIndexed { index, line ->
            // Association references only the observations retained in this export, including ID gaps.
            val mapped = mapper.restore(local[index], records[index].metadata.recordId)
            FlatJson.encode(FlatJson.decode(line) + canonicalValues(mapped))
        }.joinToString("\n", postfix = "\n")
    }

    private fun canonicalValues(record: CanonicalPoseRecord): Map<String, Any?> = linkedMapOf(
        "canonical_record_id" to record.metadata.recordId,
        "canonical_producer" to record.metadata.producer,
        "canonical_app_build" to record.metadata.appBuild,
        "canonical_run_token" to record.mappingContext.runToken,
        "canonical_profile_availability" to record.metadata.testProfile.availability.name,
        "canonical_profile_reason" to record.metadata.testProfile.reason,
        "canonical_source_clock_instance_token" to record.timing.sourceTimestamp?.domain?.instanceToken,
        "canonical_source_time_meaning" to record.timing.sourceTimestamp?.meaning?.name,
        "canonical_segment_session_id" to record.segment.sessionId,
        "canonical_coordinate_frame_session_id" to record.coordinateFrame.sessionId,
        "canonical_spatial_association_availability" to record.association.lastObservedGeometry.availability.name,
        "canonical_pose_observation_id" to record.association.poseObservationId.value,
    )

    fun encode(records: List<PoseRecord>, dataOrigin: PoseDataOrigin): String {
        validate(records)
        return records.joinToString("\n", postfix = "\n") { r ->
            FlatJson.encode(linkedMapOf(
                "schema_version" to SCHEMA, "protocol_version" to PROTOCOL, "test_profile" to "pose_debug_only",
                "data_origin" to dataOrigin.name, "source" to "ARCORE", "coordinates" to COORDINATES,
                "orientation" to "HAMILTON_XYZW", "session_id" to r.sessionId,
                "segment_id" to r.identity.segmentId, "coordinate_frame_id" to r.identity.coordinateFrameId,
                "received_at_monotonic_ns" to r.receivedAtMonotonicNs, "measurement_at_monotonic_ns" to null,
                "age_known" to false, "kind" to r.kind.name, "tracking" to r.tracking.name,
                "source_timestamp_ns" to r.sourceTimestamp?.timestampNs,
                "source_clock_domain" to r.sourceTimestamp?.clockDomain?.name,
                "source_event_meaning" to r.sourceTimestamp?.eventMeaning?.name,
                "x_m" to r.pose?.position?.xM, "y_m" to r.pose?.position?.yM, "z_m" to r.pose?.position?.zM,
                "qx" to r.pose?.orientation?.x, "qy" to r.pose?.orientation?.y, "qz" to r.pose?.orientation?.z,
                "qw" to r.pose?.orientation?.w, "reason" to r.reason?.name, "tracking_failure" to r.trackingFailure,
                "previous_segment_id" to r.previousIdentity?.segmentId,
                "previous_coordinate_frame_id" to r.previousIdentity?.coordinateFrameId,
            ))
        }
    }

    fun decode(jsonl: String): PoseReplay {
        require(jsonl.length <= 16_000_000) { "Replay size limit" }
        val lines = jsonl.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.size in 2..20_000)
        var origin: PoseDataOrigin? = null
        val canonicalRows = mutableListOf<Map<String, Any?>>()
        var enriched: Boolean? = null
        val records = lines.map { line ->
            require(line.length < 4096)
            val m = FlatJson.decode(line)
            val hasCanonical = m.keys == fields + canonicalFields
            require(m.keys == fields || hasCanonical) { "Unexpected or missing debug fields" }
            require(enriched == null || enriched == hasCanonical); enriched = hasCanonical
            if (hasCanonical) canonicalRows += m.filterKeys { it in canonicalFields }
            require(m["schema_version"] == SCHEMA && m["protocol_version"] == PROTOCOL)
            require(m["test_profile"] == "pose_debug_only" && m["source"] == "ARCORE")
            require(m["coordinates"] == COORDINATES && m["orientation"] == "HAMILTON_XYZW")
            require(m["measurement_at_monotonic_ns"] == null && m["age_known"] == false)
            val dataOrigin = PoseDataOrigin.valueOf(m.string("data_origin"))
            require(origin == null || origin == dataOrigin); origin = dataOrigin
            val sourceNs = m.numberOrNull("source_timestamp_ns")?.long()
            val source = if (sourceNs == null) {
                require(m["source_clock_domain"] == null && m["source_event_meaning"] == null)
                null
            } else PoseSourceTimestamp(sourceNs, PoseSourceClock.valueOf(m.string("source_clock_domain")),
                PoseSourceMeaning.valueOf(m.string("source_event_meaning")))
            val components = listOf("x_m", "y_m", "z_m", "qx", "qy", "qz", "qw").map { m.numberOrNull(it)?.double() }
            val pose = if (components.all { it == null }) null else {
                require(components.all { it != null })
                PhysicalPose(PositionM(components[0]!!, components[1]!!, components[2]!!),
                    QuaternionXyzw(components[3]!!, components[4]!!, components[5]!!, components[6]!!))
            }
            val previousSegment = m.stringOrNull("previous_segment_id")
            val previousFrame = m.stringOrNull("previous_coordinate_frame_id")
            require((previousSegment == null) == (previousFrame == null))
            PoseRecord(
                m.string("session_id"), PoseFrameIdentity(m.string("segment_id"), m.string("coordinate_frame_id")),
                m.number("received_at_monotonic_ns").long(), PoseRecordKind.valueOf(m.string("kind")),
                PoseTracking.valueOf(m.string("tracking")), source, pose,
                m.stringOrNull("reason")?.let(PoseReason::valueOf), m.stringOrNull("tracking_failure"),
                previousSegment?.let { PoseFrameIdentity(it, previousFrame!!) },
            )
        }
        validate(records)
        val canonical = if (enriched == true) {
            val first = canonicalRows.first()
            val context = PoseMappingContext(records.first().sessionId, first.string("canonical_run_token"),
                first.string("canonical_app_build"), PoseDebugMetadata(origin!!))
            val mapper = PoseCanonicalMapper(context)
            val usedIds = mutableSetOf<String>()
            records.mapIndexed { index, record ->
                val row = canonicalRows[index]
                val id = row.string("canonical_record_id")
                require(usedIds.add(id))
                val mapped = mapper.restore(record, id)
                require(row == canonicalValues(mapped)) { "Canonical projection disagrees with debug evidence" }
                mapped
            }
        } else null
        return PoseReplay(origin!!, records, canonical)
    }

    private fun validate(records: List<PoseRecord>) {
        require(records.size in 2..20_000)
        require(records.first().kind == PoseRecordKind.START && records.first().tracking == PoseTracking.STARTING)
        require(records.last().kind == PoseRecordKind.STOP && records.last().tracking == PoseTracking.STOPPED)
        require(records.first().reason == PoseReason.STARTED && records.last().reason != null)
        val session = records.first().sessionId
        var identity = records.first().identity
        val segments = mutableSetOf(identity.segmentId)
        val frames = mutableSetOf(identity.coordinateFrameId)
        var time = records.first().receivedAtMonotonicNs
        var state = PoseTracking.STARTING
        var sampleSourceNs: Long? = null
        records.forEachIndexed { index, r ->
            require(r.sessionId == session && r.receivedAtMonotonicNs >= time)
            time = r.receivedAtMonotonicNs
            require(index == 0 || r.kind != PoseRecordKind.START)
            require(index == records.lastIndex || r.kind != PoseRecordKind.STOP)
            if (r.kind == PoseRecordKind.BOUNDARY) {
                require(r.previousIdentity == identity && r.reason != null)
                require(segments.add(r.identity.segmentId))
                if (r.reason == PoseReason.SEGMENT_CHANGED) {
                    require(r.identity.coordinateFrameId == identity.coordinateFrameId)
                } else {
                    require(frames.add(r.identity.coordinateFrameId))
                    sampleSourceNs = null
                }
                identity = r.identity
            }
            require(r.identity == identity)
            when (r.kind) {
                PoseRecordKind.TRACKING -> { require(r.reason != null); state = r.tracking }
                PoseRecordKind.SAMPLE -> {
                    require(state == PoseTracking.TRACKING && r.tracking == state)
                    val sourceNs = r.sourceTimestamp!!.timestampNs
                    require(sampleSourceNs == null || sourceNs > sampleSourceNs)
                    sampleSourceNs = sourceNs
                }
                PoseRecordKind.STOP -> state = PoseTracking.STOPPED
                else -> require(r.tracking == state)
            }
        }
    }
}

private data class JsonNumber(val raw: String) {
    fun long(): Long = raw.toLongOrNull() ?: throw IllegalArgumentException("Integer nanoseconds required")
    fun double(): Double = raw.toDouble().also { require(it.isFinite()) }
}
private fun Map<String, Any?>.string(key: String) = this[key] as? String ?: throw IllegalArgumentException("String required")
private fun Map<String, Any?>.stringOrNull(key: String): String? = this[key]?.let { it as? String ?: throw IllegalArgumentException("String required") }
private fun Map<String, Any?>.number(key: String) = this[key] as? JsonNumber ?: throw IllegalArgumentException("Number required")
private fun Map<String, Any?>.numberOrNull(key: String): JsonNumber? = this[key]?.let { it as? JsonNumber ?: throw IllegalArgumentException("Number required") }

/** Bounded flat JSON objects only: a small debug codec avoids a runtime serialization dependency. */
private object FlatJson {
    fun encode(values: Map<String, Any?>): String = values.entries.joinToString(",", "{", "}") { (k, v) ->
        val encoded = when (v) {
            null -> "null"
            is String -> quote(v)
            is JsonNumber -> v.raw
            is Boolean, is Long, is Double -> v.toString()
            else -> error("Unsupported JSON value")
        }
        "${quote(k)}:$encoded"
    }
    private fun quote(s: String): String = buildString {
        append('"')
        s.forEach { c -> when (c) {
            '"' -> append("\\\""); '\\' -> append("\\\\")
            else -> if (c.code < 32) append("\\u" + c.code.toString(16).padStart(4, '0')) else append(c)
        } }
        append('"')
    }
    fun decode(s: String): Map<String, Any?> = Parser(s).read()
    private class Parser(private val s: String) {
        private var i = 0
        private fun whitespace() { while (i < s.length && s[i] in " \t\r\n") i++ }
        private fun expect(c: Char) { whitespace(); require(i < s.length && s[i++] == c) }
        private fun string(): String {
            expect('"')
            return buildString {
                while (i < s.length) {
                    val c = s[i++]
                    if (c == '"') return@buildString
                    require(c.code >= 32)
                    if (c != '\\') append(c) else {
                        require(i < s.length)
                        when (val escape = s[i++]) {
                            '"', '\\', '/' -> append(escape)
                            'b' -> append('\b'); 'f' -> append('\u000c'); 'n' -> append('\n'); 'r' -> append('\r'); 't' -> append('\t')
                            'u' -> { require(i + 4 <= s.length); append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                            else -> throw IllegalArgumentException("Invalid escape")
                        }
                    }
                }
                throw IllegalArgumentException("Unterminated string")
            }
        }
        fun read(): Map<String, Any?> {
            val m = linkedMapOf<String, Any?>()
            expect('{'); whitespace()
            if (i < s.length && s[i] == '}') { i++; whitespace(); require(i == s.length); return m }
            while (true) {
                val key = string(); require(!m.containsKey(key)); expect(':'); whitespace()
                require(i < s.length)
                val value: Any? = if (s[i] == '"') string() else {
                    val start = i
                    while (i < s.length && s[i] !in ",} \t\r\n") i++
                    when (val token = s.substring(start, i)) {
                        "null" -> null; "true" -> true; "false" -> false
                        else -> { require(token.matches(Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))); JsonNumber(token) }
                    }
                }
                m[key] = value; whitespace(); require(i < s.length)
                if (s[i] == '}') { i++; break }
                expect(',')
            }
            whitespace(); require(i == s.length)
            return m
        }
    }
}
