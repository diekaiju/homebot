package com.tk.quicksearch.search.files

import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.FileTypeUtils
import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.FileClassifier
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.search.utils.RecentResultRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.Locale

object FileSearchAlgorithm {

    fun search(
            fullList: List<DeviceFile>,
            query: String,
            enabledFileTypes: Set<FileType>,
            excludedFileUris: Set<String>,
            excludedFileExtensions: Set<String>,
            folderWhitelistPatterns: Set<String>,
            folderBlacklistPatterns: Set<String>,
            showFolders: Boolean,
            showSystemFiles: Boolean,
            fileNicknames: Map<String, String?>,
            recentFileScores: Map<String, Int> = emptyMap(),
            resultLimit: Int = 25,
    ): List<DeviceFile> {
        val normalizedQuery = SearchTextNormalizer.normalizeQueryWhitespace(query)
        val filteredFiles =
                filterCandidates(
                        fullList = fullList,
                        query = query,
                        enabledFileTypes = enabledFileTypes,
                        excludedFileUris = excludedFileUris,
                        excludedFileExtensions = excludedFileExtensions,
                        folderWhitelistPatterns = folderWhitelistPatterns,
                        folderBlacklistPatterns = folderBlacklistPatterns,
                        showFolders = showFolders,
                        showSystemFiles = showSystemFiles,
                )

        return rankFiles(
                filteredFiles,
                SearchQueryContext.fromRawQuery(normalizedQuery),
                fileNicknames,
                recentFileScores,
        ).take(resultLimit)
    }

    fun search(
            fullList: List<DeviceFile>,
            queryContext: SearchQueryContext,
            enabledFileTypes: Set<FileType>,
            excludedFileUris: Set<String>,
            excludedFileExtensions: Set<String>,
            folderWhitelistPatterns: Set<String>,
            folderBlacklistPatterns: Set<String>,
            showFolders: Boolean,
            showSystemFiles: Boolean,
            fileNicknames: Map<String, String?>,
            recentFileScores: Map<String, Int> = emptyMap(),
            resultLimit: Int = 25,
    ): List<DeviceFile> {
        val filteredFiles =
                filterCandidates(
                        fullList = fullList,
                        query = queryContext.normalizedQuery,
                        enabledFileTypes = enabledFileTypes,
                        excludedFileUris = excludedFileUris,
                        excludedFileExtensions = excludedFileExtensions,
                        folderWhitelistPatterns = folderWhitelistPatterns,
                        folderBlacklistPatterns = folderBlacklistPatterns,
                        showFolders = showFolders,
                        showSystemFiles = showSystemFiles,
                )

        return rankFiles(filteredFiles, queryContext, fileNicknames, recentFileScores).take(resultLimit)
    }

    fun filterCandidates(
            fullList: List<DeviceFile>,
            query: String,
            enabledFileTypes: Set<FileType>,
            excludedFileUris: Set<String>,
            excludedFileExtensions: Set<String>,
            folderWhitelistPatterns: Set<String>,
            folderBlacklistPatterns: Set<String>,
            showFolders: Boolean,
            showSystemFiles: Boolean,
    ): List<DeviceFile> {
        if (query.isBlank()) return emptyList()

        val normalizedQuery = SearchTextNormalizer.normalizeQueryWhitespace(query)
        if (normalizedQuery.length < 2) return emptyList()

        val pathMatcher =
                FolderPathPatternMatcher.createPathMatcher(
                        whitelistPatterns = folderWhitelistPatterns,
                        blacklistPatterns = folderBlacklistPatterns,
                )

        return fullList.filter { file ->
            val isSystem = FileClassifier.isSystemFolder(file) || FileClassifier.isSystemFile(file)

            if (file.isDirectory) {
                if (!showFolders) return@filter false
            } else {
                val fileType = FileTypeUtils.getFileType(file)
                if (fileType !in enabledFileTypes) return@filter false
                if (fileType == FileType.OTHER && isSystem) return@filter false

                val isApk = isApkFile(file)
                if (isApk && FileType.APKS !in enabledFileTypes) return@filter false
            }

            if (isSystem && !showSystemFiles) return@filter false

            val isHidden = file.displayName.startsWith(".")
            if (isHidden && !showSystemFiles) return@filter false

            if (!showSystemFiles && FileClassifier.isInTrashFolder(file)) return@filter false

            !excludedFileUris.contains(file.uri.toString()) &&
                    pathMatcher(file) &&
                    !FileUtils.isFileExtensionExcluded(
                            file.displayName,
                            excludedFileExtensions,
                    )
        }
    }

    private fun rankFiles(
            files: List<DeviceFile>,
            queryContext: SearchQueryContext,
            fileNicknames: Map<String, String?>,
            recentFileScores: Map<String, Int>,
    ): List<DeviceFile> {
        if (files.isEmpty()) return emptyList()

        return files
                .distinctBy { it.uri.toString() }
                .mapNotNull { file ->
                    val uriString = file.uri.toString()
                    val nickname = fileNicknames[uriString]
                    val priority =
                            FileSearchPolicy.matchPriority(file.displayName, nickname, queryContext)
                    if (!DefaultSearchMatcher.isMatch(priority)) {
                        null
                    } else {
                        file to priority
                    }
                }
                .sortedWith(
                        RecentResultRankingUtils.matchThenRecencyThenAlphabeticalComparator(
                                recencyScores = recentFileScores,
                                keySelector = { it.uri.toString() },
                                labelSelector = { it.displayName },
                        ),
                )
                .map { it.first }
    }

    private fun isApkFile(deviceFile: DeviceFile): Boolean {
        val mime = deviceFile.mimeType?.lowercase(Locale.getDefault())
        if (mime == "application/vnd.android.package-archive") {
            return true
        }
        val name = deviceFile.displayName.lowercase(Locale.getDefault())
        return name.endsWith(".apk")
    }
}
