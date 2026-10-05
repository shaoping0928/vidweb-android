package com.vidweb.android.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface VidWebApi {

    @GET("api/config")
    suspend fun config(): AppConfig

    /* browse */
    @GET("api/drives")
    suspend fun drives(): DrivesResponse

    @GET("api/browse")
    suspend fun browse(
        @Query("path") path: String,
        @Query("filter") filter: String = "",
        @Query("only_video") onlyVideo: Boolean = false,
        @Query("sort") sort: Int = 0,
    ): BrowseResponse

    @POST("api/explore")
    suspend fun explore(@Body body: PathBody): OkResponse

    /* dirs */
    @GET("api/dirs")
    suspend fun dirs(): DirsResponse

    @POST("api/dirs")
    suspend fun addDir(@Body body: PathBody): DirMutationResponse

    @PUT("api/dirs/{id}")
    suspend fun updateDir(@Path("id") id: Int, @Body body: PathBody): DirMutationResponse

    @DELETE("api/dirs/{id}")
    suspend fun deleteDir(@Path("id") id: Int): DirMutationResponse

    /* scan */
    @POST("api/scan")
    suspend fun scan(@Body body: ScanBody): ScanStartResponse

    @GET("api/scan/status")
    suspend fun scanStatus(@Query("job_id") jobId: String): ScanStatus

    @POST("api/scan/cancel")
    suspend fun scanCancel(@Body body: JobBody): OkResponse

    /* library */
    @POST("api/playlist/query")
    suspend fun queryPlaylist(@Body body: PlaylistQueryRequest): PlaylistQueryResponse

    @GET("api/playlist")
    suspend fun playlist(): PathsResponse

    @PUT("api/playlist")
    suspend fun setPlaylist(@Body body: PathsBody): OkCountResponse

    @POST("api/playlist/add")
    suspend fun playlistAdd(@Body body: PathsBody): OkCountResponse

    @POST("api/playlist/remove")
    suspend fun playlistRemove(@Body body: PathsBody): OkCountResponse

    @POST("api/playlist/clean")
    suspend fun playlistClean(): OkCountResponse

    @POST("api/playlist/restore")
    suspend fun playlistRestore(): OkCountResponse

    @DELETE("api/playlist")
    suspend fun playlistClear(): OkResponse

    @POST("api/playlist/filter")
    suspend fun playlistFilter(@Body body: FilterBody): PathsResponse

    @POST("api/playlist/save")
    suspend fun playlistSave(@Body body: M3uSaveBody): OkResponse

    @POST("api/playlist/load")
    suspend fun playlistLoad(@Body body: M3uLoadBody): PathsResponse

    /* files */
    @GET("api/files/detail")
    suspend fun fileDetail(@Query("path") path: String): FileDetail

    @GET("api/files/probe")
    suspend fun probe(@Query("path") path: String): ProbeInfo

    @GET("api/files/all")
    suspend fun filesAll(@Query("limit") limit: Int = 20000): FilesInfoResponse

    @POST("api/files/detect_broken")
    suspend fun detectBroken(): ScanStartResponse

    @POST("api/files/info")
    suspend fun filesInfo(@Body body: PathsBody): FilesInfoResponse

    @POST("api/files/progress")
    suspend fun saveProgress(@Body body: ProgressBody): OkResponse

    @POST("api/files/played")
    suspend fun played(@Body body: PathBody): OkResponse

    @POST("api/files/media_info")
    suspend fun mediaInfo(@Body body: MediaInfoBody): OkResponse

    /* tags */
    @GET("api/tags")
    suspend fun tags(): TagsListResponse

    @POST("api/tags")
    suspend fun createTag(@Body body: TagCreateBody): TagCreateResponse

    @PUT("api/tags/{id}")
    suspend fun updateTag(@Path("id") id: Int, @Body body: TagUpdateBody): OkResponse

    @DELETE("api/tags/{id}")
    suspend fun deleteTag(@Path("id") id: Int): OkResponse

    @GET("api/tags/{id}/files")
    suspend fun tagFiles(@Path("id") id: Int): PathsResponse

    @GET("api/files/tags")
    suspend fun fileTags(@Query("path") path: String): TagsResponse

    @PUT("api/files/tags")
    suspend fun setFileTags(@Body body: FileTagsBody): TagsResponse

    @POST("api/files/tags/common")
    suspend fun commonTags(@Body body: PathsBody): CommonTagsResponse

    @POST("api/files/tags/batch")
    suspend fun batchTags(@Body body: TagsBatchBody): OkResponse

    /* collections */
    @GET("api/collections")
    suspend fun collections(): CollectionsResponse

    @GET("api/collections/counts")
    suspend fun collectionCounts(): CollectionCountsResponse

    @POST("api/collections")
    suspend fun createCollection(@Body body: CollectionBody): CollectionResponse

    @PUT("api/collections/{id}")
    suspend fun updateCollection(@Path("id") id: Int, @Body body: CollectionUpdateBody): CollectionResponse

    @DELETE("api/collections/{id}")
    suspend fun deleteCollection(@Path("id") id: Int): OkResponse

    /* comments */
    @GET("api/comments")
    suspend fun comments(@Query("path") path: String): CommentsResponse

    @POST("api/comments")
    suspend fun addComment(@Body body: CommentAddBody): CommentItem

    @PUT("api/comments/{id}")
    suspend fun updateComment(@Path("id") id: Int, @Body body: CommentUpdateBody): OkResponse

    @DELETE("api/comments/{id}")
    suspend fun deleteComment(@Path("id") id: Int): OkResponse

    /* search / recent */
    @GET("api/search")
    suspend fun search(@Query("q") q: String, @Query("limit") limit: Int = 200): SearchResponse

    @GET("api/recent")
    suspend fun recent(@Query("limit") limit: Int = 50): RecentResponse

    @POST("api/recent/clear")
    suspend fun clearRecent(): OkResponse

    /* thumbs */
    @POST("api/thumbs")
    suspend fun requestThumbs(@Body body: PathsBody): OkResponse

    @GET("api/thumbs/status")
    suspend fun thumbsStatus(@Query("path") path: String): ThumbsStatus

    /* screenshots */
    @GET("api/screenshots")
    suspend fun screenshots(
        @Query("path") path: String = "",
        @Query("limit") limit: Int = 200,
    ): ScreenshotsResponse

    @Multipart
    @POST("api/screenshots")
    suspend fun uploadScreenshot(
        @Part file: MultipartBody.Part,
        @Part("path") path: RequestBody,
        @Part("position_ms") positionMs: RequestBody,
    ): Screenshot

    @DELETE("api/screenshots/{id}")
    suspend fun deleteScreenshot(@Path("id") id: Int): OkResponse

    /* transcode */
    @GET("api/transcode/info")
    suspend fun transcodeInfo(): TranscodeInfo

    @GET("api/transcode/plan")
    suspend fun transcodePlan(@Query("path") path: String): TranscodePlan

    /* tasks */
    @GET("api/tasks")
    suspend fun tasks(@Query("limit") limit: Int = 100): TasksResponse

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): OkResponse

    /* settings */
    @GET("api/settings")
    suspend fun settings(): SettingsData

    @PUT("api/settings")
    suspend fun updateSettings(@Body body: SettingsUpdateBody): SettingsUpdateResponse

    @POST("api/settings/test-tool")
    suspend fun testTool(@Body body: TestToolBody): TestToolResponse

    /* state */
    @GET("api/state")
    suspend fun state(): StateResponse

    @PUT("api/state")
    suspend fun putState(@Body body: StatePutBody): OkResponse

    @DELETE("api/state/{key}")
    suspend fun deleteState(@Path("key") key: String): OkResponse
}

object ApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Volatile
    private var base: String = "http://127.0.0.1:8765/"

    @Volatile
    private var apiRef: VidWebApi? = null

    val api: VidWebApi
        get() = apiRef ?: synchronized(this) {
            apiRef ?: build(base).also { apiRef = it }
        }

    fun setBaseUrl(url: String) {
        val normalized = normalize(url)
        synchronized(this) {
            if (normalized != base) {
                base = normalized
                apiRef = build(base)
            }
        }
    }

    fun baseUrl(): String = base

    fun streamUrl(path: String): String =
        base + "api/stream?path=" + enc(path)

    fun transcodeStreamUrl(path: String, startMs: Long): String =
        base + "api/transcode/stream?path=" + enc(path) + "&start=" + startMs.coerceAtLeast(0)

    fun thumbUrl(path: String, bust: Long = 0): String =
        base + "api/thumbnail?path=" + enc(path) + if (bust > 0) "&_=$bust" else ""

    fun previewUrl(path: String, seconds: Double): String =
        base + "api/preview?path=" + enc(path) + "&t=" + seconds

    fun screenshotImageUrl(id: Int): String = base + "api/screenshots/$id/image"

    fun normalize(url: String): String {
        var value = url.trim()
        if (value.isEmpty()) return "http://127.0.0.1:8765/"
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://$value"
        }
        if (!value.endsWith("/")) value += "/"
        return value
    }

    private fun enc(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    private fun build(baseUrl: String): VidWebApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(VidWebApi::class.java)
    }
}
