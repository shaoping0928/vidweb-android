package com.vidweb.android.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/* ---------------------------------------------------------------- config */

@Serializable
data class AppConfig(
    val name: String = "",
    val version: String = "",
    val ffmpeg: Boolean = false,
    val ffprobe: Boolean = false,
    @SerialName("ffmpeg_path") val ffmpegPath: String = "",
    val transcode: Boolean = false,
    val roots: List<String> = emptyList(),
    @SerialName("data_dir") val dataDir: String = "",
    @SerialName("default_m3u") val defaultM3u: String = "",
    @SerialName("video_exts") val videoExts: List<String> = emptyList(),
)

/* ---------------------------------------------------------------- browse */

@Serializable
data class DriveItem(
    val path: String = "",
    val name: String = "",
    val total: Long = 0,
    val free: Long = 0,
)

@Serializable
data class DrivesResponse(val roots: List<DriveItem> = emptyList())

@Serializable
data class BrowseEntry(
    val name: String = "",
    val path: String = "",
    val isdir: Boolean = false,
    @SerialName("is_video") val isVideo: Boolean = false,
    val size: Long = 0,
    val mtime: Long = 0,
    @SerialName("has_thumb") val hasThumb: Boolean = false,
)

@Serializable
data class BrowseResponse(
    val path: String = "",
    val parent: String = "",
    val entries: List<BrowseEntry> = emptyList(),
)

/* ---------------------------------------------------------------- dirs */

@Serializable
data class DirItem(val id: Int = 0, val path: String = "")

@Serializable
data class DirsResponse(val items: List<DirItem> = emptyList())

@Serializable
data class DirMutationResponse(
    val ok: Boolean = false,
    @SerialName("moved_files") val movedFiles: Int = 0,
    val detached: Int = 0,
    val items: List<DirItem> = emptyList(),
)

/* ---------------------------------------------------------------- library */

@Serializable
data class MediaItem(
    val path: String = "",
    val name: String = "",
    val size: Long = 0,
    val mtime: Long = 0,
    val exists: Boolean = true,
    val broken: Boolean = false,
    @SerialName("duration_ms") val durationMs: Long = 0,
    @SerialName("last_position_ms") val lastPositionMs: Long = 0,
    @SerialName("play_count") val playCount: Int = 0,
    @SerialName("last_played_at") val lastPlayedAt: Long = 0,
    val tags: List<String> = emptyList(),
    @SerialName("has_thumb") val hasThumb: Boolean = false,
)

@Serializable
data class TreeNode(
    val name: String = "",
    val path: String = "",
    val count: Int = 0,
    val children: List<TreeNode> = emptyList(),
)

@Serializable
data class PlaylistQueryResponse(
    val items: List<MediaItem> = emptyList(),
    val tree: List<TreeNode> = emptyList(),
    val paths: List<String> = emptyList(),
    val total: Int = 0,
    val filtered: Int = 0,
    val offset: Int = 0,
    val limit: Int = 30,
    val folder: String? = null,
)

@Serializable
data class PlaylistQueryRequest(
    val offset: Int = 0,
    val limit: Int = 30,
    val search: String = "",
    val status: String = "all",
    val statuses: List<String>? = null,
    val duration: String = "all",
    val durations: List<String>? = null,
    @SerialName("tag_ids") val tagIds: List<Int> = emptyList(),
    @SerialName("tag_match_all") val tagMatchAll: Boolean = false,
    val sort: String = "name",
    @SerialName("paths_only") val pathsOnly: Boolean = false,
    val tree: Boolean = false,
    val folder: String? = null,
)

@Serializable
data class ScanBody(val path: String = "", @SerialName("dir_id") val dirId: Int? = null)

@Serializable
data class ScanStartResponse(@SerialName("job_id") val jobId: String = "")

@Serializable
data class ScanStatus(
    @SerialName("job_id") val jobId: String = "",
    val path: String = "",
    val state: String = "",
    val total: Int = 0,
    val processed: Int = 0,
    val added: Int = 0,
    @SerialName("playlist_added") val playlistAdded: Int = 0,
    val error: String? = null,
    val elapsed: Double = 0.0,
)

/* ---------------------------------------------------------------- files */

@Serializable
data class FileRecord(
    val id: Int = 0,
    val path: String = "",
    val name: String? = null,
    val title: String? = null,
    val size: Long = 0,
    val mtime: Long = 0,
    @SerialName("duration_ms") val durationMs: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    @SerialName("last_position_ms") val lastPositionMs: Long = 0,
    @SerialName("play_count") val playCount: Int = 0,
    @SerialName("last_played_at") val lastPlayedAt: Long = 0,
    val thumbnail: String? = null,
)

@Serializable
data class FileDetail(
    val exists: Boolean = false,
    val path: String = "",
    val name: String = "",
    val size: Long = 0,
    val mtime: Long = 0,
    val record: FileRecord? = null,
    val tags: List<Tag> = emptyList(),
    @SerialName("comment_count") val commentCount: Int = 0,
    val thumbnail: String? = null,
)

@Serializable
data class ProbeInfo(
    val video: String = "",
    val audio: String = "",
    val profile: String = "",
    val container: String = "",
    val playable: Boolean? = null,
)

/* ---------------------------------------------------------------- tags */

@Serializable
data class Tag(
    val id: Int = 0,
    val name: String = "",
    val color: String? = null,
    @SerialName("parent_id") val parentId: Int? = null,
    @SerialName("file_count") val fileCount: Int = 0,
    @SerialName("subtree_count") val subtreeCount: Int = 0,
)

@Serializable
data class TagsResponse(val tags: List<Tag> = emptyList())

@Serializable
data class TagsListResponse(val items: List<Tag> = emptyList())

@Serializable
data class TagCreateResponse(val id: Int = 0, val name: String = "")

@Serializable
data class CommonTagsResponse(
    val tags: List<Tag> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
)

/* ---------------------------------------------------------------- collections */

@Serializable
data class CollectionItem(
    val id: Int = 0,
    val name: String = "",
    val filters: JsonObject = JsonObject(emptyMap()),
    @SerialName("created_at") val createdAt: Long = 0,
)

@Serializable
data class CollectionsResponse(val items: List<CollectionItem> = emptyList())

@Serializable
data class CollectionResponse(val item: CollectionItem = CollectionItem())

@Serializable
data class CollectionCountsResponse(val counts: Map<String, Int> = emptyMap())

/* ---------------------------------------------------------------- comments */

@Serializable
data class CommentItem(
    val id: Int = 0,
    @SerialName("file_id") val fileId: Int = 0,
    @SerialName("position_ms") val positionMs: Long = 0,
    val text: String = "",
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
)

@Serializable
data class CommentsResponse(val items: List<CommentItem> = emptyList())

/* ---------------------------------------------------------------- tasks */

@Serializable
data class TaskItem(
    val id: Int = 0,
    @SerialName("job_id") val jobId: String = "",
    val type: String = "",
    val name: String = "",
    val description: String = "",
    val ip: String = "",
    val state: String = "",
    val total: Int = 0,
    val processed: Int = 0,
    val added: Int = 0,
    val error: String? = null,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("finished_at") val finishedAt: Long = 0,
)

@Serializable
data class TasksResponse(val items: List<TaskItem> = emptyList())

/* ---------------------------------------------------------------- screenshots */

@Serializable
data class Screenshot(
    val id: Int = 0,
    @SerialName("file_id") val fileId: Int = 0,
    @SerialName("video_path") val videoPath: String = "",
    @SerialName("position_ms") val positionMs: Long = 0,
    val filename: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val size: Long = 0,
    @SerialName("created_at") val createdAt: Long = 0,
)

@Serializable
data class ScreenshotsResponse(val items: List<Screenshot> = emptyList())

/* ---------------------------------------------------------------- search / recent */

@Serializable
data class RecentItem(
    val path: String = "",
    val name: String = "",
    val exists: Boolean = true,
    @SerialName("play_count") val playCount: Int = 0,
    @SerialName("last_played_at") val lastPlayedAt: Long = 0,
    @SerialName("last_position_ms") val lastPositionMs: Long = 0,
    @SerialName("duration_ms") val durationMs: Long = 0,
    @SerialName("has_thumb") val hasThumb: Boolean = false,
)

@Serializable
data class SearchResponse(val items: List<RecentItem> = emptyList())

@Serializable
data class RecentResponse(val items: List<RecentItem> = emptyList())

/* ---------------------------------------------------------------- transcode */

@Serializable
data class TranscodeInfo(
    val enabled: Boolean = false,
    val selected: String = "auto",
    val available: List<EncoderOption> = emptyList(),
    val encoder: String = "",
    @SerialName("encoder_label") val encoderLabel: String = "",
)

@Serializable
data class EncoderOption(val key: String = "", val label: String = "")

@Serializable
data class TranscodePlan(
    val mode: String = "direct",
    val video: String = "",
    val audio: String = "",
    val encoder: String = "",
    @SerialName("encoder_label") val encoderLabel: String = "",
)

/* ---------------------------------------------------------------- settings */

@Serializable
data class SettingsData(
    val host: String = "",
    val port: Int = 8765,
    @SerialName("allowed_roots") val allowedRoots: List<String> = emptyList(),
    @SerialName("effective_roots") val effectiveRoots: List<String> = emptyList(),
    val ffmpeg: String = "",
    val ffprobe: String = "",
    val transcode: Boolean = false,
    @SerialName("transcode_encoder") val transcodeEncoder: String = "auto",
    @SerialName("access_log") val accessLog: Boolean = false,
    @SerialName("ffmpeg_resolved") val ffmpegResolved: String = "",
    @SerialName("ffprobe_resolved") val ffprobeResolved: String = "",
    @SerialName("data_dir") val dataDir: String = "",
    @SerialName("config_file") val configFile: String = "",
    @SerialName("playlist_dir") val playlistDir: String = "",
)

@Serializable
data class SettingsUpdateBody(
    val host: String? = null,
    val port: Int? = null,
    @SerialName("allowed_roots") val allowedRoots: List<String>? = null,
    val ffmpeg: String? = null,
    val ffprobe: String? = null,
    val transcode: Boolean? = null,
    @SerialName("transcode_encoder") val transcodeEncoder: String? = null,
    @SerialName("access_log") val accessLog: Boolean? = null,
)

@Serializable
data class SettingsUpdateResponse(
    val ok: Boolean = false,
    @SerialName("restart_required") val restartRequired: Boolean = false,
    val ffmpeg: Boolean = false,
    val ffprobe: Boolean = false,
)

@Serializable
data class TestToolBody(val path: String = "", val tool: String = "ffmpeg")

@Serializable
data class TestToolResponse(
    val ok: Boolean = false,
    val resolved: String = "",
    val version: String = "",
)

/* ---------------------------------------------------------------- state */

@Serializable
data class StateResponse(val items: Map<String, String> = emptyMap())

@Serializable
data class StatePutBody(
    val key: String? = null,
    val value: String? = null,
    val items: Map<String, String>? = null,
)

/* ---------------------------------------------------------------- generic bodies */

@Serializable
data class PathBody(val path: String = "")

@Serializable
data class PathsBody(val paths: List<String> = emptyList())

@Serializable
data class M3uSaveBody(val file: String = "", val paths: List<String> = emptyList())

@Serializable
data class M3uLoadBody(val file: String = "")

@Serializable
data class JobBody(@SerialName("job_id") val jobId: String = "")

@Serializable
data class ProgressBody(val path: String = "", @SerialName("position_ms") val positionMs: Long = 0)

@Serializable
data class MediaInfoBody(
    val path: String = "",
    @SerialName("duration_ms") val durationMs: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val title: String = "",
)

@Serializable
data class FileTagsBody(val path: String = "", val tags: List<String> = emptyList())

@Serializable
data class TagsBatchBody(val paths: List<String> = emptyList(), val tags: List<String> = emptyList())

@Serializable
data class FilterBody(
    val paths: List<String> = emptyList(),
    @SerialName("tag_ids") val tagIds: List<Int> = emptyList(),
    @SerialName("match_all") val matchAll: Boolean = false,
)

@Serializable
data class CommentAddBody(
    val path: String = "",
    @SerialName("position_ms") val positionMs: Long = 0,
    val text: String = "",
)

@Serializable
data class CommentUpdateBody(val text: String = "")

@Serializable
data class TagCreateBody(val name: String = "", @SerialName("parent_id") val parentId: Int? = null)

@Serializable
data class TagUpdateBody(
    val name: String? = null,
    val color: String? = null,
    @SerialName("parent_id") val parentId: Int? = null,
)

@Serializable
data class CollectionBody(val name: String = "", val filters: JsonObject = JsonObject(emptyMap()))

@Serializable
data class CollectionUpdateBody(val name: String? = null, val filters: JsonObject? = null)

@Serializable
data class OkResponse(val ok: Boolean = false)

@Serializable
data class OkCountResponse(
    val ok: Boolean = false,
    val added: Int = 0,
    val removed: Int = 0,
    val total: Int = 0,
    val count: Int = 0,
)

@Serializable
data class PathsResponse(val paths: List<String> = emptyList(), val items: List<String> = emptyList())

@Serializable
data class ThumbsStatus(val status: String = "", val cached: String? = null)

@Serializable
data class FilesInfoResponse(val items: List<MediaItem> = emptyList())
