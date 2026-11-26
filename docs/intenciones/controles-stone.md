# Índice de controles de Stone

539 coincidencias estáticas de callbacks, gestos y acciones de Android en 84 archivos. «Por cruzar» indica que el punto está localizado y falta asignarle un ID validado. Las coincidencias incluyen wiring y callbacks reutilizables, por lo que su número no equivale al de botones únicos. Consulta el [mapa de acciones](stone.md) para contratos y permisos.

<details>
<summary>data/library/notifications/NewFilesNotifier.kt (1)</summary>

- `private fun openApp(context: Context) = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)` · stone.navigation.open_notification. [NewFilesNotifier.kt:69](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/data/library/notifications/NewFilesNotifier.kt#L69)

</details>

<details>
<summary>data/library/notifications/NotificationScheduler.kt (1)</summary>

- `val intent = PendingIntent.getBroadcast(context, code, Intent(context, NotificationAlarmReceiver::class.java).setAction(action),` · Por cruzar. [NotificationScheduler.kt:39](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/data/library/notifications/NotificationScheduler.kt#L39)

</details>

<details>
<summary>feature/library/LibraryScreen.kt (17)</summary>

- `BackHandler(enabled = collection != null || query.isNotEmpty()) {` · Por cruzar. [LibraryScreen.kt:173](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L173)
- `onHome = { tab = LibraryTab.Songs; collection = null; query = ""; onHome() },` · stone.navigation.open_library. [LibraryScreen.kt:183](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L183)
- `onAction = dispatchQuickAction, onEqualizer = onEqualizer,` · Por cruzar. [LibraryScreen.kt:184](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L184)
- `onSettings = { onOpenSettings(); onSettingsSheetVisible(true) })` · stone.navigation.open_settings. [LibraryScreen.kt:185](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L185)
- `onQuickAction = dispatchQuickAction, onMoreQuickActions = { moreQuickActions = true },` · Por cruzar. [LibraryScreen.kt:198](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L198)
- `onRemoveFromPlaylist, onDelete, onAudioOnly, onOpenFolders = {` · Por cruzar. [LibraryScreen.kt:213](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L213)
- `onVideoOptions = onVideoOptions,` · Por cruzar. [LibraryScreen.kt:217](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L217)
- `onVideoLibraryLayoutChange = onVideoLibraryLayoutChange,` · Por cruzar. [LibraryScreen.kt:219](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L219)
- `onSetPlaylistCover = onSetPlaylistCover,` · Por cruzar. [LibraryScreen.kt:220](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L220)
- `onRestorePlaylistCover = onRestorePlaylistCover,` · Por cruzar. [LibraryScreen.kt:221](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L221)
- `onLandscapeLayoutChange = onLandscapeLayoutChange, videoScale = videoScale,` · Por cruzar. [LibraryScreen.kt:232](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L232)
- `onVideoScaleChange = onVideoScaleChange,` · Por cruzar. [LibraryScreen.kt:233](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L233)
- `onVideoControlChange = onVideoControlChange,` · Por cruzar. [LibraryScreen.kt:234](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L234)
- `onImmersiveEnabledChange = onImmersiveEnabledChange,` · Por cruzar. [LibraryScreen.kt:236](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L236)
- `onShakeToReportChange = onShakeToReportChange) { key, visible ->` · Por cruzar. [LibraryScreen.kt:238](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L238)
- `if (moreQuickActions) ModalBottomSheet(onDismissRequest = { moreQuickActions = false }) {` · Por cruzar. [LibraryScreen.kt:242](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L242)
- `TextButton(onClick = { dispatchQuickAction(action.id) }, modifier = Modifier.fillMaxWidth()) {` · Por cruzar. [LibraryScreen.kt:247](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/LibraryScreen.kt#L247)

</details>

<details>
<summary>feature/library/components/AlbumCard.kt (1)</summary>

- `.clickable(onClick = open).padding(12.dp).height(112.dp),` · Por cruzar. [AlbumCard.kt:32](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/AlbumCard.kt#L32)

</details>

<details>
<summary>feature/library/components/ArtistCard.kt (1)</summary>

- `.clickable(onClick = open).padding(12.dp).height(112.dp),` · Por cruzar. [ArtistCard.kt:29](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/ArtistCard.kt#L29)

</details>

<details>
<summary>feature/library/components/GenreCards.kt (1)</summary>

- `Box(modifier.height(110.dp).clip(RoundedCornerShape(10.dp)).background(color).clickable(onClick = onClick)) {` · Por cruzar. [GenreCards.kt:31](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/GenreCards.kt#L31)

</details>

<details>
<summary>feature/library/components/HomeNavigationRail.kt (4)</summary>

- `Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onHome)` · Por cruzar. [HomeNavigationRail.kt:37](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/HomeNavigationRail.kt#L37)
- `"equalizer" in activeIds, onClick = onEqualizer)` · Por cruzar. [HomeNavigationRail.kt:48](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/HomeNavigationRail.kt#L48)
- `onClick = onSettings)` · Por cruzar. [HomeNavigationRail.kt:51](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/HomeNavigationRail.kt#L51)
- `.clickable(enabled = enabled, onClick = onClick)` · Por cruzar. [HomeNavigationRail.kt:59](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/HomeNavigationRail.kt#L59)

</details>

<details>
<summary>feature/library/components/LandscapeLibraryHeading.kt (2)</summary>

- `IconButton(onClick = onFolder, enabled = !scanning) {` · Por cruzar. [LandscapeLibraryHeading.kt:30](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LandscapeLibraryHeading.kt#L30)
- `IconButton(onClick = onOpenFolders) {` · Por cruzar. [LandscapeLibraryHeading.kt:33](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LandscapeLibraryHeading.kt#L33)

</details>

<details>
<summary>feature/library/components/LayoutSelector.kt (1)</summary>

- `.clickable { onChange(layout) }.semantics { contentDescription = description },` · Por cruzar. [LayoutSelector.kt:46](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LayoutSelector.kt#L46)

</details>

<details>
<summary>feature/library/components/LibraryContent.kt (23)</summary>

- `androidx.activity.compose.BackHandler(enabled = managing) { managing = false; selectedIds = emptySet() }` · Por cruzar. [LibraryContent.kt:76](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L76)
- `IconButton(onClick = onFolder, enabled = !state.scanning) {` · Por cruzar. [LibraryContent.kt:85](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L85)
- `IconButton(onClick = onOpenFolders) {` · Por cruzar. [LibraryContent.kt:88](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L88)
- `IconButton(onClick = onFolder, enabled = !state.scanning) {` · Por cruzar. [LibraryContent.kt:101](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L101)
- `SelectionHeader(selectedIds.size, visible.size, onExit = { managing = false; selectedIds = emptySet() },` · Por cruzar. [LibraryContent.kt:107](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L107)
- `onToggleAll = {` · Por cruzar. [LibraryContent.kt:108](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L108)
- `IconButton(onClick = { menuVisible = true }) {` · Por cruzar. [LibraryContent.kt:115](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L115)
- `Row(Modifier.fillMaxWidth().clickable { select(name) }.padding(vertical = 12.dp),` · Por cruzar. [LibraryContent.kt:148](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L148)
- `onEdit = if (managing && selectedIds.size == 1 && song.id in selectedIds) {` · Por cruzar. [LibraryContent.kt:167](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L167)
- `onLongClick = { if (!managing) { managing = true; selectedIds = setOf(song.id) } },` · Por cruzar. [LibraryContent.kt:170](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L170)
- `onClick = {` · Por cruzar. [LibraryContent.kt:171](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L171)
- `if (managing) Row(Modifier.fillMaxWidth().clickable {` · Por cruzar. [LibraryContent.kt:182](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L182)
- `onLongClick = { managing = true; selectedIds = setOf(song.id) }) { onPlay(song, visible) }` · Por cruzar. [LibraryContent.kt:194](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L194)
- `onAddTo = { pickPlaylist = true },` · Por cruzar. [LibraryContent.kt:200](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L200)
- `onPlayNext = { selectionActions.onPlayNext(selectedSongs); managing = false; selectedIds = emptySet() },` · Por cruzar. [LibraryContent.kt:201](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L201)
- `onShare = { runCatching { com.zirakusa.stone.nativeapp.data.library.user.SongActions.shareMany(context, selectedSongs) } },` · Por cruzar. [LibraryContent.kt:202](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L202)
- `onDelete = { deleteConfirmVisible = true })` · Por cruzar. [LibraryContent.kt:203](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L203)
- `if (menuVisible) ModalBottomSheet(onDismissRequest = { menuVisible = false }) {` · Por cruzar. [LibraryContent.kt:205](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L205)
- `TextButton(onClick = { menuVisible = false; managing = true }) {` · Por cruzar. [LibraryContent.kt:208](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L208)
- `TextButton(onClick = {` · Por cruzar. [LibraryContent.kt:211](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L211)
- `if (deleteConfirmVisible) AlertDialog(onDismissRequest = { deleteConfirmVisible = false },` · Por cruzar. [LibraryContent.kt:224](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L224)
- `confirmButton = { TextButton(onClick = {` · Por cruzar. [LibraryContent.kt:227](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L227)
- `dismissButton = { TextButton(onClick = { deleteConfirmVisible = false }) {` · Por cruzar. [LibraryContent.kt:231](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryContent.kt#L231)

</details>

<details>
<summary>feature/library/components/LibraryHeader.kt (4)</summary>

- `Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).clickable {` · Por cruzar. [LibraryHeader.kt:52](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryHeader.kt#L52)
- `onClick = { onQuickAction(action.id) },` · Por cruzar. [LibraryHeader.kt:70](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryHeader.kt#L70)
- `if (quickActions.size > 8) IconButton(onClick = onMoreQuickActions, modifier = Modifier.size(44.dp)) {` · Por cruzar. [LibraryHeader.kt:81](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryHeader.kt#L81)
- `if (showEqualizer) IconButton(onClick = onEqualizer, modifier = Modifier.size(44.dp)) {` · Por cruzar. [LibraryHeader.kt:88](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryHeader.kt#L88)

</details>

<details>
<summary>feature/library/components/LibrarySelection.kt (7)</summary>

- `IconButton(onClick = onExit) { Icon(Icons.Default.ArrowBackIosNew, stringResource(R.string.back)) }` · Por cruzar. [LibrarySelection.kt:29](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySelection.kt#L29)
- `Row(Modifier.fillMaxWidth().clickable(onClick = onToggleAll).padding(vertical = 8.dp),` · stone.library.select_tracks. [LibrarySelection.kt:33](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySelection.kt#L33)
- `Column(Modifier.weight(1f).clickable(enabled = enabled, onClick = onClick).padding(vertical = 6.dp),` · Por cruzar. [LibrarySelection.kt:59](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySelection.kt#L59)
- `AlertDialog(onDismissRequest = close,` · Por cruzar. [LibrarySelection.kt:70](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySelection.kt#L70)
- `TextButton(onClick = { songs.forEach { onAdd(playlist.id, it) }; close() }) { Text(playlist.name) }` · stone.playlist.add_songs. [LibrarySelection.kt:76](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySelection.kt#L76)
- `confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } })` · Por cruzar. [LibrarySelection.kt:80](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySelection.kt#L80)
- `IconButton(onClick = onClick) { Icon(Icons.Default.Edit, stringResource(R.string.tag_editor), tint = StonePalette.Secondary) }` · Por cruzar. [LibrarySelection.kt:85](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySelection.kt#L85)

</details>

<details>
<summary>feature/library/components/LibrarySheets.kt (3)</summary>

- `ModalBottomSheet(onDismissRequest = dismiss, containerColor = StonePalette.Chip) {` · Por cruzar. [LibrarySheets.kt:35](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySheets.kt#L35)
- `Row(Modifier.fillMaxWidth().clickable { onSort(item) }.padding(vertical = 12.dp),` · Por cruzar. [LibrarySheets.kt:42](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySheets.kt#L42)
- `Row(Modifier.fillMaxWidth().clickable(onClick = action).padding(vertical = 18.dp),` · Por cruzar. [LibrarySheets.kt:54](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySheets.kt#L54)

</details>

<details>
<summary>feature/library/components/LibrarySongRow.kt (2)</summary>

- `.combinedClickable(enabled = enabled || onLongClick != null, onLongClick = onLongClick,` · Por cruzar. [LibrarySongRow.kt:38](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySongRow.kt#L38)
- `onClick = { if (enabled) play() })` · Por cruzar. [LibrarySongRow.kt:39](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibrarySongRow.kt#L39)

</details>

<details>
<summary>feature/library/components/LibraryTabs.kt (2)</summary>

- `.selectable(active, role = Role.Tab, onClick = { onSelect(tab) })` · Por cruzar. [LibraryTabs.kt:45](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryTabs.kt#L45)
- `.selectable(active, role = Role.Tab, onClick = { onSelect(tab) })` · Por cruzar. [LibraryTabs.kt:52](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/LibraryTabs.kt#L52)

</details>

<details>
<summary>feature/library/components/PlaylistCards.kt (2)</summary>

- `.combinedClickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally,` · Por cruzar. [PlaylistCards.kt:44](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistCards.kt#L44)
- `.combinedClickable(onClick = open, onLongClick = onCoverOptions).padding(8.dp),` · Por cruzar. [PlaylistCards.kt:61](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistCards.kt#L61)

</details>

<details>
<summary>feature/library/components/PlaylistLibrary.kt (12)</summary>

- `val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->` · Por cruzar. [PlaylistLibrary.kt:40](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L40)
- `compact, open = { select(id) }, onCoverOptions = { coverTarget = id })` · Por cruzar. [PlaylistLibrary.kt:65](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L65)
- `compact, open = { select(playlist.id) }, onCoverOptions = { coverTarget = playlist.id })` · stone.playlist.songs. [PlaylistLibrary.kt:70](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L70)
- `IconButton(onClick = { select(null) }) { Icon(Icons.Default.ArrowBack, stringResource(R.string.back)) }` · stone.playlist.set_cover. [PlaylistLibrary.kt:75](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L75)
- `FilledIconButton(enabled = currentTracks.isNotEmpty(), onClick = { onPlay(currentTracks.first(), currentTracks) }) {` · stone.playlist.play. [PlaylistLibrary.kt:78](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L78)
- `if (collection.startsWith("custom-")) IconButton(onClick = { onRemove(collection, song) }) {` · stone.playlist.remove_songs. [PlaylistLibrary.kt:85](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L85)
- `if (createVisible) AlertDialog(onDismissRequest = { createVisible = false },` · Por cruzar. [PlaylistLibrary.kt:91](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L91)
- `confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name); name = ""; createVisible = false }) {` · stone.playlist.create. [PlaylistLibrary.kt:94](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L94)
- `Text(stringResource(R.string.create)) } }, dismissButton = { TextButton(onClick = { createVisible = false }) {` · Por cruzar. [PlaylistLibrary.kt:95](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L95)
- `onDismissRequest = { coverTarget = null },` · Por cruzar. [PlaylistLibrary.kt:101](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L101)
- `TextButton(onClick = { coverPickTarget = target; coverTarget = null; coverPicker.launch("image/*") }) {` · stone.playlist.set_cover. [PlaylistLibrary.kt:105](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L105)
- `TextButton(enabled = hasCustom, onClick = {` · Por cruzar. [PlaylistLibrary.kt:110](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/PlaylistLibrary.kt#L110)

</details>

<details>
<summary>feature/library/components/RecentlyDeletedSheet.kt (8)</summary>

- `val systemDelete = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->` · Por cruzar. [RecentlyDeletedSheet.kt:44](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/RecentlyDeletedSheet.kt#L44)
- `ModalBottomSheet(onDismissRequest = dismiss, containerColor = StonePalette.Chip,` · Por cruzar. [RecentlyDeletedSheet.kt:49](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/RecentlyDeletedSheet.kt#L49)
- `if (songs.isNotEmpty()) TextButton(onClick = { pending = songs },` · Por cruzar. [RecentlyDeletedSheet.kt:59](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/RecentlyDeletedSheet.kt#L59)
- `AlertDialog(onDismissRequest = { pending = null },` · Por cruzar. [RecentlyDeletedSheet.kt:75](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/RecentlyDeletedSheet.kt#L75)
- `confirmButton = { TextButton(onClick = {` · Por cruzar. [RecentlyDeletedSheet.kt:78](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/RecentlyDeletedSheet.kt#L78)
- `dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) } })` · Por cruzar. [RecentlyDeletedSheet.kt:93](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/RecentlyDeletedSheet.kt#L93)
- `IconButton(onClick = restore) {` · Por cruzar. [RecentlyDeletedSheet.kt:107](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/RecentlyDeletedSheet.kt#L107)
- `IconButton(onClick = deleteForever) {` · Por cruzar. [RecentlyDeletedSheet.kt:110](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/RecentlyDeletedSheet.kt#L110)

</details>

<details>
<summary>feature/library/components/SongGridCard.kt (1)</summary>

- `Column(Modifier.fillMaxWidth().combinedClickable(onLongClick = onLongClick, onClick = onClick)) {` · Por cruzar. [SongGridCard.kt:28](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/components/SongGridCard.kt#L28)

</details>

<details>
<summary>feature/library/video/VideoGridCard.kt (6)</summary>

- `onClick = options,` · Por cruzar. [VideoGridCard.kt:114](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoGridCard.kt#L114)
- `Column(Modifier.fillMaxWidth().combinedClickable(onLongClick = onLongClick, onClick = play)) {` · Por cruzar. [VideoGridCard.kt:143](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoGridCard.kt#L143)
- `onOptions = onOptions.takeIf { checked == null },` · Por cruzar. [VideoGridCard.kt:148](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoGridCard.kt#L148)
- `Modifier.fillMaxWidth().combinedClickable(onLongClick = onLongClick, onClick = play)` · Por cruzar. [VideoGridCard.kt:178](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoGridCard.kt#L178)
- `onOptions = null,` · Por cruzar. [VideoGridCard.kt:187](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoGridCard.kt#L187)
- `else if (checked == null) IconButton(onClick = onOptions) {` · Por cruzar. [VideoGridCard.kt:208](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoGridCard.kt#L208)

</details>

<details>
<summary>feature/library/video/VideoLibraryContent.kt (11)</summary>

- `SelectionHeader(selection.ids.size, visible.size, onExit = selection::exit, onToggleAll = {` · Por cruzar. [VideoLibraryContent.kt:47](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoLibraryContent.kt#L47)
- `if (selectedFolder != null) IconButton(onClick = { selectedFolder = null; folderBrowser = true }) {` · Por cruzar. [VideoLibraryContent.kt:52](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoLibraryContent.kt#L52)
- `IconButton(onClick = onFolder, enabled = !state.scanning) {` · Por cruzar. [VideoLibraryContent.kt:59](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoLibraryContent.kt#L59)
- `IconButton(onClick = {` · Por cruzar. [VideoLibraryContent.kt:62](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoLibraryContent.kt#L62)
- `Row(Modifier.fillMaxWidth().clickable { selectedFolder = folder; folderBrowser = false }` · Por cruzar. [VideoLibraryContent.kt:73](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoLibraryContent.kt#L73)
- `onOptions = { onVideoOptions(video) }, checked = selection.checked(video),` · Por cruzar. [VideoLibraryContent.kt:98](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoLibraryContent.kt#L98)
- `onLongClick = { if (!selection.active) selection.start(video) },` · Por cruzar. [VideoLibraryContent.kt:99](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoLibraryContent.kt#L99)
- `onEdit = edit)` · Por cruzar. [VideoLibraryContent.kt:100](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoLibraryContent.kt#L100)
- `onOptions = { onVideoOptions(video) }, checked = selection.checked(video),` · Por cruzar. [VideoLibraryContent.kt:111](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoLibraryContent.kt#L111)
- `onLongClick = { if (!selection.active) selection.start(video) },` · Por cruzar. [VideoLibraryContent.kt:112](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoLibraryContent.kt#L112)
- `onEdit = if (selection.editable(video)) { { selection.editing = video } } else null,` · Por cruzar. [VideoLibraryContent.kt:113](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoLibraryContent.kt#L113)

</details>

<details>
<summary>feature/library/video/VideoSelection.kt (7)</summary>

- `BackHandler(enabled = selection.active) { selection.exit() }` · Por cruzar. [VideoSelection.kt:38](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoSelection.kt#L38)
- `if (selection.confirmDelete) AlertDialog(onDismissRequest = { selection.confirmDelete = false },` · Por cruzar. [VideoSelection.kt:43](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoSelection.kt#L43)
- `confirmButton = { TextButton(onClick = {` · Por cruzar. [VideoSelection.kt:46](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoSelection.kt#L46)
- `dismissButton = { TextButton(onClick = { selection.confirmDelete = false }) { Text(stringResource(R.string.cancel)) } })` · Por cruzar. [VideoSelection.kt:50](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoSelection.kt#L50)
- `onAddTo = {}, onPlayNext = {},` · Por cruzar. [VideoSelection.kt:56](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoSelection.kt#L56)
- `onShare = { runCatching { SongActions.shareMany(context, videos.filter { it.id in selection.ids }) } },` · Por cruzar. [VideoSelection.kt:57](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoSelection.kt#L57)
- `onDelete = { selection.confirmDelete = true })` · Por cruzar. [VideoSelection.kt:58](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/library/video/VideoSelection.kt#L58)

</details>

<details>
<summary>feature/onboarding/OnboardingScreen.kt (5)</summary>

- `val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onFinished() }` · Por cruzar. [OnboardingScreen.kt:46](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/onboarding/OnboardingScreen.kt#L46)
- `val media = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {` · Por cruzar. [OnboardingScreen.kt:47](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/onboarding/OnboardingScreen.kt#L47)
- `.clickable(remember { MutableInteractionSource() }, indication = null) {}` · Por cruzar. [OnboardingScreen.kt:54](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/onboarding/OnboardingScreen.kt#L54)
- `if (!asking) Button(onClick = { asking = true; media.launch(DeviceMediaAccess.permissions()) },` · stone.settings.onboarding_access. [OnboardingScreen.kt:62](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/onboarding/OnboardingScreen.kt#L62)
- `Row(Modifier.fillMaxWidth().clickable { if (!active) onSelect(code) }.padding(horizontal = 24.dp, vertical = 18.dp),` · Por cruzar. [OnboardingScreen.kt:75](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/onboarding/OnboardingScreen.kt#L75)

</details>

<details>
<summary>feature/player/CompactPlayerControls.kt (10)</summary>

- `Slider(value = dragging ?: progress.first.coerceIn(0, end).toFloat(),` · Por cruzar. [CompactPlayerControls.kt:34](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/CompactPlayerControls.kt#L34)
- `onValueChange = { dragging = it }, valueRange = 0f..end.coerceAtLeast(1).toFloat(),` · Por cruzar. [CompactPlayerControls.kt:35](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/CompactPlayerControls.kt#L35)
- `enabled = end > 0, onValueChangeFinished = {` · stone.player.restart_current. [CompactPlayerControls.kt:36](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/CompactPlayerControls.kt#L36)
- `IconButton(onClick = actions.previous, enabled = state.ready) {` · stone.player.previous. [CompactPlayerControls.kt:54](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/CompactPlayerControls.kt#L54)
- `FilledIconButton(onClick = actions.toggle, enabled = state.ready, modifier = Modifier.size(56.dp)) {` · stone.player.pause. [CompactPlayerControls.kt:57](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/CompactPlayerControls.kt#L57)
- `IconButton(onClick = actions.next, enabled = state.ready) {` · stone.player.next. [CompactPlayerControls.kt:61](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/CompactPlayerControls.kt#L61)
- `IconButton(onClick = actions.lyrics) { Icon(Icons.Default.ChatBubbleOutline, stringResource(R.string.lyrics)) }` · stone.library.lyrics. [CompactPlayerControls.kt:68](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/CompactPlayerControls.kt#L68)
- `IconButton(onClick = actions.queue) { Icon(Icons.Default.QueueMusic, stringResource(R.string.queue)) }` · stone.player.queue. [CompactPlayerControls.kt:69](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/CompactPlayerControls.kt#L69)
- `IconToggleButton(checked = state.shuffle, onCheckedChange = { actions.shuffle() }, enabled = state.ready) {` · stone.player.set_shuffle. [CompactPlayerControls.kt:76](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/CompactPlayerControls.kt#L76)
- `IconButton(onClick = actions.repeat, enabled = state.ready) {` · stone.player.set_repeat. [CompactPlayerControls.kt:83](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/CompactPlayerControls.kt#L83)

</details>

<details>
<summary>feature/player/LandscapePlayerPanel.kt (2)</summary>

- `IconButton(onClick = actions.options) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }` · Por cruzar. [LandscapePlayerPanel.kt:52](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapePlayerPanel.kt#L52)
- `Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = open)` · Por cruzar. [LandscapePlayerPanel.kt:61](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapePlayerPanel.kt#L61)

</details>

<details>
<summary>feature/player/LandscapeVinylPlayer.kt (11)</summary>

- `if (song != null) VinylDisc(song, state.playing, progress, onScrub = { scrubMs = it },` · Por cruzar. [LandscapeVinylPlayer.kt:63](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapeVinylPlayer.kt#L63)
- `onRelease = { target -> playback.seek(target); scrubMs = null },` · Por cruzar. [LandscapeVinylPlayer.kt:64](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapeVinylPlayer.kt#L64)
- `IconButton(onClick = onEqualizer) { Icon(Icons.Default.Tune, stringResource(R.string.equalizer)) }` · Por cruzar. [LandscapeVinylPlayer.kt:74](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapeVinylPlayer.kt#L74)
- `IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, stringResource(R.string.queue)) }` · Por cruzar. [LandscapeVinylPlayer.kt:75](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapeVinylPlayer.kt#L75)
- `IconButton(onClick = onOptions) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }` · Por cruzar. [LandscapeVinylPlayer.kt:76](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapeVinylPlayer.kt#L76)
- `IconButton(onClick = back) { Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.close_player)) }` · Por cruzar. [LandscapeVinylPlayer.kt:77](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapeVinylPlayer.kt#L77)
- `.pointerInput(song?.id) { detectTapGestures { onLyrics() } })` · Por cruzar. [LandscapeVinylPlayer.kt:80](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapeVinylPlayer.kt#L80)
- `detectDragGestures(` · Por cruzar. [LandscapeVinylPlayer.kt:119](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapeVinylPlayer.kt#L119)
- `onDragStart = { start ->` · Por cruzar. [LandscapeVinylPlayer.kt:120](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapeVinylPlayer.kt#L120)
- `onDragEnd = { dragging = false; onRelease(target) },` · Por cruzar. [LandscapeVinylPlayer.kt:125](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapeVinylPlayer.kt#L125)
- `onDragCancel = { dragging = false; onRelease(target) },` · Por cruzar. [LandscapeVinylPlayer.kt:126](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LandscapeVinylPlayer.kt#L126)

</details>

<details>
<summary>feature/player/LibraryPlaybackPane.kt (6)</summary>

- `onSelectVideo = onSelectVideo,` · Por cruzar. [LibraryPlaybackPane.kt:52](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LibraryPlaybackPane.kt#L52)
- `onOpenFullscreen = { onOpen?.invoke() },` · Por cruzar. [LibraryPlaybackPane.kt:53](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LibraryPlaybackPane.kt#L53)
- `onAudioOnly = onAudioOnly,` · Por cruzar. [LibraryPlaybackPane.kt:54](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LibraryPlaybackPane.kt#L54)
- `onOptions = {` · Por cruzar. [LibraryPlaybackPane.kt:55](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LibraryPlaybackPane.kt#L55)
- `onQueue = { sheets.queueVisible = true },` · Por cruzar. [LibraryPlaybackPane.kt:60](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LibraryPlaybackPane.kt#L60)
- `onScaleToggle = {` · Por cruzar. [LibraryPlaybackPane.kt:61](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/LibraryPlaybackPane.kt#L61)

</details>

<details>
<summary>feature/player/MiniPlayer.kt (4)</summary>

- `.pointerInput(state.currentId) { detectMiniPlayerSwipe(previous, next, onSwipeUp) }` · stone.player.previous. [MiniPlayer.kt:60](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/MiniPlayer.kt#L60)
- `.clickable(onClick = open),` · stone.navigation.open_player. [MiniPlayer.kt:61](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/MiniPlayer.kt#L61)
- `if (onPlayMode != null) IconButton(onClick = onPlayMode) {` · stone.player.set_repeat. [MiniPlayer.kt:83](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/MiniPlayer.kt#L83)
- `private suspend fun PointerInputScope.detectMiniPlayerSwipe(onPrevious: () -> Unit,` · Por cruzar. [MiniPlayer.kt:106](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/MiniPlayer.kt#L106)

</details>

<details>
<summary>feature/player/MiniPlayerOptionsPanel.kt (3)</summary>

- `BackHandler(onBack = onClose)` · Por cruzar. [MiniPlayerOptionsPanel.kt:29](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/MiniPlayerOptionsPanel.kt#L29)
- `.clickable(onClick = onClose))` · Por cruzar. [MiniPlayerOptionsPanel.kt:32](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/MiniPlayerOptionsPanel.kt#L32)
- `onSelect = onSelect` · Por cruzar. [MiniPlayerOptionsPanel.kt:58](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/MiniPlayerOptionsPanel.kt#L58)

</details>

<details>
<summary>feature/player/PlayerProgress.kt (5)</summary>

- `val verticalGesture = if (onVerticalDrag == null) Modifier else Modifier.pointerInput(onVerticalDrag) {` · Por cruzar. [PlayerProgress.kt:53](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerProgress.kt#L53)
- `detectVerticalDragGestures { change, dragAmount ->` · Por cruzar. [PlayerProgress.kt:54](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerProgress.kt#L54)
- `Slider(value = dragging ?: progress.first.toFloat().coerceAtMost(progress.second.toFloat()),` · Por cruzar. [PlayerProgress.kt:59](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerProgress.kt#L59)
- `onValueChange = { dragging = it }, valueRange = 0f..progress.second.toFloat().coerceAtLeast(1f),` · Por cruzar. [PlayerProgress.kt:60](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerProgress.kt#L60)
- `onValueChangeFinished = {` · Por cruzar. [PlayerProgress.kt:62](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerProgress.kt#L62)

</details>

<details>
<summary>feature/player/PlayerScreen.kt (27)</summary>

- `BackHandler(onBack = back)` · stone.navigation.open_library. [PlayerScreen.kt:75](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L75)
- `detectTrackSwipe(playback::previous, playback::next, onUpFromBottom = { sheets.lyricsVisible = true },` · stone.library.lyrics. [PlayerScreen.kt:103](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L103)
- `onUpFromMiddle = onRecommendations)` · stone.player.open_recommendations. [PlayerScreen.kt:104](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L104)
- `onAudioOnly = onAudioOnly, videoScale = videoScale,` · stone.video.gestures_and_pip. [PlayerScreen.kt:113](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L113)
- `onVideoScaleChange = onVideoScaleChange,` · Por cruzar. [PlayerScreen.kt:114](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L114)
- `onOptions = { sheets.optionsSong = null; sheets.optionsSidePanel = false; sheets.optionsVisible = true }, onQueue = { sheets.queueVisible = true },` · Por cruzar. [PlayerScreen.kt:116](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L116)
- `onSpeed = { sheets.tool = PlayerTool.Speed }, onTool = { sheets.tool = it },` · Por cruzar. [PlayerScreen.kt:117](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L117)
- `onOptions = { sheets.optionsSong = null; sheets.optionsSidePanel = false; sheets.optionsVisible = true }, onLyrics = { sheets.lyricsVisible = true },` · Por cruzar. [PlayerScreen.kt:121](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L121)
- `onEqualizer = { sheets.tool = PlayerTool.Equalizer },` · Por cruzar. [PlayerScreen.kt:122](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L122)
- `onQueue = { sheets.queueVisible = true })` · Por cruzar. [PlayerScreen.kt:123](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L123)
- `onOptions = { sheets.optionsSong = null; sheets.optionsSidePanel = false; sheets.optionsVisible = true }, onQueue = { sheets.queueVisible = true },` · Por cruzar. [PlayerScreen.kt:125](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L125)
- `onAudioOnly = {` · Por cruzar. [PlayerScreen.kt:126](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L126)
- `onVideoScaleChange = onVideoScaleChange, videoControls = videoControls,` · Por cruzar. [PlayerScreen.kt:129](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L129)
- `onSpeed = { sheets.tool = PlayerTool.Speed })` · Por cruzar. [PlayerScreen.kt:130](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L130)
- `IconButton(onClick = back, Modifier.align(Alignment.CenterStart)) {` · Por cruzar. [PlayerScreen.kt:138](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L138)
- `onClick = { sheets.tool = PlayerTool.Sleep })` · stone.player.sleep_timer. [PlayerScreen.kt:149](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L149)
- `.pointerInput(song.id) { detectTapGestures { audioControls.show() } })` · Por cruzar. [PlayerScreen.kt:160](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L160)
- `.pointerInput(song.id) { detectTapGestures { audioControls.show() } })` · Por cruzar. [PlayerScreen.kt:163](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L163)
- `.pointerInput(Unit) { detectTapGestures { audioControls.show() } })` · Por cruzar. [PlayerScreen.kt:165](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L165)
- `modifier = Modifier.padding(top = 5.dp).clickable(` · Por cruzar. [PlayerScreen.kt:171](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L171)
- `enabled = state.artist.isNotBlank(), onClick = onArtist), maxLines = 1)` · Por cruzar. [PlayerScreen.kt:172](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L172)
- `IconButton(onClick = { sheets.optionsSong = null; sheets.optionsVisible = true }) {` · Por cruzar. [PlayerScreen.kt:178](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L178)
- `IconButton(onClick = { sheets.tool = PlayerTool.Equalizer }) {` · Por cruzar. [PlayerScreen.kt:200](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L200)
- `IconButton(onClick = { sheets.queueVisible = true }) { Icon(Icons.Default.Menu, stringResource(R.string.queue)) }` · stone.player.queue. [PlayerScreen.kt:209](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L209)
- `onAddToPlaylist, onDelete, onMetadataChanged, onEditComplete = back,` · Por cruzar. [PlayerScreen.kt:224](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L224)
- `extraActions = playerOptionExtras, videoScale = videoScale, onVideoScaleChange = onVideoScaleChange)` · Por cruzar. [PlayerScreen.kt:225](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L225)
- `private suspend fun PointerInputScope.detectTrackSwipe(onPrevious: () -> Unit, onNext: () -> Unit,` · Por cruzar. [PlayerScreen.kt:234](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/PlayerScreen.kt#L234)

</details>

<details>
<summary>feature/player/lyrics/LyricsAutoSearch.kt (4)</summary>

- `AlertDialog(onDismissRequest = onDismiss,` · Por cruzar. [LyricsAutoSearch.kt:36](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsAutoSearch.kt#L36)
- `confirmButton = { TextButton(onClick = onWeb) { Text(stringResource(R.string.lyrics_auto_web)) } },` · Por cruzar. [LyricsAutoSearch.kt:52](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsAutoSearch.kt#L52)
- `dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })` · Por cruzar. [LyricsAutoSearch.kt:53](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsAutoSearch.kt#L53)
- `Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {` · Por cruzar. [LyricsAutoSearch.kt:59](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsAutoSearch.kt#L59)

</details>

<details>
<summary>feature/player/lyrics/LyricsSearchBrowser.kt (2)</summary>

- `IconButton(onClick = { browserView?.takeIf { it.canGoBack() }?.goBack() ?: onClose() }) {` · Por cruzar. [LyricsSearchBrowser.kt:64](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSearchBrowser.kt#L64)
- `Button(enabled = browserView != null, onClick = {` · Por cruzar. [LyricsSearchBrowser.kt:104](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSearchBrowser.kt#L104)

</details>

<details>
<summary>feature/player/lyrics/LyricsSheet.kt (18)</summary>

- `BackHandler(enabled = browserVisible) { browserVisible = false }` · Por cruzar. [LyricsSheet.kt:90](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L90)
- `if (browserVisible) Dialog(onDismissRequest = { browserVisible = false },` · Por cruzar. [LyricsSheet.kt:97](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L97)
- `LyricsSearchBrowser(song, onClose = { browserVisible = false }, onLyrics = ::openInEditor)` · Por cruzar. [LyricsSheet.kt:101](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L101)
- `if (autoSearch) LyricsAutoSearchDialog(song, ::openInEditor, onWeb = { autoSearch = false; browserVisible = true },` · stone.metadata.search_lyrics. [LyricsSheet.kt:105](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L105)
- `onDismiss = { autoSearch = false })` · Por cruzar. [LyricsSheet.kt:106](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L106)
- `ModalBottomSheet(onDismissRequest = onClose, containerColor = tint, contentColor = ink,` · Por cruzar. [LyricsSheet.kt:111](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L111)
- `IconButton(onClick = { editor = false }) { Icon(Icons.Default.ArrowBack, stringResource(R.string.back)) }` · Por cruzar. [LyricsSheet.kt:118](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L118)
- `IconButton(onClick = { autoSearch = true }) { Icon(Icons.Default.AutoAwesome, stringResource(R.string.lyrics_auto_title)) }` · stone.metadata.search_lyrics. [LyricsSheet.kt:123](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L123)
- `IconButton(onClick = { browserVisible = true }) { Icon(Icons.Default.TravelExplore, stringResource(R.string.search_lyrics)) }` · Por cruzar. [LyricsSheet.kt:124](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L124)
- `TextButton(onClick = {` · Por cruzar. [LyricsSheet.kt:125](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L125)
- `Button(enabled = draft.text.isNotBlank(), onClick = {` · stone.metadata.set_lyrics. [LyricsSheet.kt:133](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L133)
- `IconButton(onClick = playback::toggle) {` · stone.player.pause. [LyricsSheet.kt:157](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L157)
- `onClick = { playback.seek(line.timeMs); autoScroll = true },` · stone.lyrics.toggle_autoscroll. [LyricsSheet.kt:175](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L175)
- `OutlinedIconButton(onClick = { editor = true }, Modifier.size(62.dp)) { Icon(Icons.Default.Add, stringResource(R.string.lyrics_add)) }` · Por cruzar. [LyricsSheet.kt:192](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L192)
- `FilledIconButton(onClick = { autoSearch = true }, Modifier.size(62.dp),` · Por cruzar. [LyricsSheet.kt:194](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L194)
- `if (document.synced.isNotEmpty()) IconButton(onClick = { autoScroll = !autoScroll }) {` · stone.lyrics.toggle_autoscroll. [LyricsSheet.kt:206](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L206)
- `IconButton(onClick = {` · Por cruzar. [LyricsSheet.kt:210](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L210)
- `IconButton(onClick = {` · Por cruzar. [LyricsSheet.kt:215](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/LyricsSheet.kt#L215)

</details>

<details>
<summary>feature/player/lyrics/PlayerLyricChip.kt (1)</summary>

- `Surface(onClick = onClick, modifier = modifier.height(56.dp),` · Por cruzar. [PlayerLyricChip.kt:40](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/lyrics/PlayerLyricChip.kt#L40)

</details>

<details>
<summary>feature/player/sheets/PlayerSheetsHost.kt (5)</summary>

- `videoScale = videoScale, onVideoScaleChange = onVideoScaleChange,` · Por cruzar. [PlayerSheetsHost.kt:35](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerSheetsHost.kt#L35)
- `onSelect = { selected ->` · Por cruzar. [PlayerSheetsHost.kt:36](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerSheetsHost.kt#L36)
- `onClose = { sheets.optionsVisible = false; sheets.optionsSong = null })` · Por cruzar. [PlayerSheetsHost.kt:42](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerSheetsHost.kt#L42)
- `onClose = {` · Por cruzar. [PlayerSheetsHost.kt:48](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerSheetsHost.kt#L48)
- `onSelect = { selected ->` · Por cruzar. [PlayerSheetsHost.kt:52](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerSheetsHost.kt#L52)

</details>

<details>
<summary>feature/player/sheets/PlayerToolsSheets.kt (32)</summary>

- `androidx.compose.ui.window.Dialog(onDismissRequest = onClose,` · Por cruzar. [PlayerToolsSheets.kt:83](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L83)
- `Box(Modifier.weight(1f).fillMaxHeight().clickable(` · Por cruzar. [PlayerToolsSheets.kt:88](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L88)
- `indication = null, onClick = onClose))` · Por cruzar. [PlayerToolsSheets.kt:90](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L90)
- `PlayerOptionsContent(state, song, extraActions = extraActions, onSelect = onSelect,` · Por cruzar. [PlayerToolsSheets.kt:93](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L93)
- `ModalBottomSheet(onDismissRequest = onClose, containerColor = tint) {` · Por cruzar. [PlayerToolsSheets.kt:100](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L100)
- `PlayerOptionsContent(state, song, extraActions = extraActions, onSelect = onSelect)` · Por cruzar. [PlayerToolsSheets.kt:102](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L102)
- `IconButton(onClick = { onSelect(PlayerTool.Tags) }) {` · Por cruzar. [PlayerToolsSheets.kt:135](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L135)
- `Row(Modifier.fillMaxWidth().height(64.dp).clickable { onSelect(row.tool) },` · Por cruzar. [PlayerToolsSheets.kt:144](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L144)
- `Row(Modifier.fillMaxWidth().height(64.dp).clickable(onClick = action.onClick),` · Por cruzar. [PlayerToolsSheets.kt:152](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L152)
- `ModalBottomSheet(onDismissRequest = close, containerColor = Color(0xFF17151B)) {` · Por cruzar. [PlayerToolsSheets.kt:165](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L165)
- `Row(Modifier.fillMaxWidth().height(58.dp).clickable { playback.setSpeed(speed); close() },` · Por cruzar. [PlayerToolsSheets.kt:170](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L170)
- `ModalBottomSheet(onDismissRequest = close, containerColor = EqSurface,` · Por cruzar. [PlayerToolsSheets.kt:211](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L211)
- `IconButton(onClick = playback::toggle) {` · stone.player.pause. [PlayerToolsSheets.kt:224](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L224)
- `if (song != null) IconButton(onClick = {` · Por cruzar. [PlayerToolsSheets.kt:229](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L229)
- `Switch(state.equalizerEnabled, playback::setEqualizerEnabled)` · stone.equalizer.set_enabled. [PlayerToolsSheets.kt:243](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L243)
- `Switch(state.equalizerByGenre, playback::setEqualizerByGenre)` · stone.equalizer.set_by_genre. [PlayerToolsSheets.kt:251](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L251)
- `TextButton(onClick = { playback.setEqualizerEnabled(false) }) {` · stone.equalizer.set_enabled. [PlayerToolsSheets.kt:254](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L254)
- `.clickable { playback.setEqualizerPreset(name, preset.values) }` · stone.equalizer.set_preset. [PlayerToolsSheets.kt:277](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L277)
- `if (enabled) detectTapGestures { point ->` · Por cruzar. [PlayerToolsSheets.kt:302](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L302)
- `if (enabled) detectVerticalDragGestures(onDragStart = { y ->` · stone.equalizer.set_band. [PlayerToolsSheets.kt:306](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L306)
- `ModalBottomSheet(onDismissRequest = close, containerColor = Color(0xFF17151B)) {` · Por cruzar. [PlayerToolsSheets.kt:346](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L346)
- `Slider(start, { start = it }, valueRange = 0f..(song.durationMs - 30_000).coerceAtLeast(0).toFloat())` · stone.player.set_ringtone. [PlayerToolsSheets.kt:351](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L351)
- `Button(enabled = !busy, onClick = {` · Por cruzar. [PlayerToolsSheets.kt:352](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L352)
- `AlertDialog(onDismissRequest = close, title = { ImmersiveWindowEffect(); Text(stringResource(R.string.delete_song_title)) },` · Por cruzar. [PlayerToolsSheets.kt:366](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L366)
- `confirmButton = { TextButton(onClick = confirm) { Text(stringResource(R.string.delete)) } },` · Por cruzar. [PlayerToolsSheets.kt:368](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L368)
- `dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } })` · Por cruzar. [PlayerToolsSheets.kt:369](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L369)
- `ModalBottomSheet(onDismissRequest = close, containerColor = Color(0xFF17151B)) {` · Por cruzar. [PlayerToolsSheets.kt:377](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L377)
- `OutlinedButton(onClick = { creating = true }, Modifier.fillMaxWidth().padding(vertical = 12.dp)) {` · Por cruzar. [PlayerToolsSheets.kt:381](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L381)
- `playlists.forEach { playlist -> Row(Modifier.fillMaxWidth().height(58.dp).clickable {` · Por cruzar. [PlayerToolsSheets.kt:385](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L385)
- `if (creating) AlertDialog(onDismissRequest = { creating = false }, title = { ImmersiveWindowEffect(); Text(stringResource(R.string.new_playlist)) },` · Por cruzar. [PlayerToolsSheets.kt:393](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L393)
- `confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { create(name); creating = false; name = "" }) {` · stone.playlist.create. [PlayerToolsSheets.kt:395](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L395)
- `Text(stringResource(R.string.create)) } }, dismissButton = { TextButton(onClick = { creating = false }) {` · Por cruzar. [PlayerToolsSheets.kt:396](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/PlayerToolsSheets.kt#L396)

</details>

<details>
<summary>feature/player/sheets/QueueSheet.kt (14)</summary>

- `ModalBottomSheet(onDismissRequest = onClose, containerColor = tint,` · Por cruzar. [QueueSheet.kt:66](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L66)
- `IconButton(onClick = { if (state.index >= 0) scope.launch { listState.animateScrollToItem(state.index) } }) {` · Por cruzar. [QueueSheet.kt:75](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L75)
- `IconButton(onClick = { clearDialog = true }) {` · stone.player.queue_clear. [QueueSheet.kt:78](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L78)
- `detectHorizontalDragGestures(onHorizontalDrag = { change, amount ->` · stone.player.queue_remove. [QueueSheet.kt:112](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L112)
- `}, onDragEnd = {` · Por cruzar. [QueueSheet.kt:118](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L118)
- `}, onDragCancel = {` · Por cruzar. [QueueSheet.kt:124](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L124)
- `.clickable { if (swipeOffset != 0f) swipeState.remove(item.id) else playback.playAt(index) }` · stone.player.queue_play. [QueueSheet.kt:135](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L135)
- `detectDragGestures(onDragStart = {` · stone.player.queue_move. [QueueSheet.kt:151](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L151)
- `}, onDragEnd = {` · Por cruzar. [QueueSheet.kt:154](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L154)
- `}, onDragCancel = {` · Por cruzar. [QueueSheet.kt:157](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L157)
- `onDismissRequest = { clearDialog = false },` · Por cruzar. [QueueSheet.kt:192](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L192)
- `TextButton(onClick = { playback.clearQueue(); clearDialog = false }) {` · stone.player.queue_clear. [QueueSheet.kt:196](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L196)
- `TextButton(onClick = { clearDialog = false }) {` · Por cruzar. [QueueSheet.kt:201](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L201)
- `Surface(onClick = onClick, modifier = modifier.height(48.dp), shape = RoundedCornerShape(24.dp),` · Por cruzar. [QueueSheet.kt:237](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/QueueSheet.kt#L237)

</details>

<details>
<summary>feature/player/sheets/RecommendationsSheet.kt (3)</summary>

- `ModalBottomSheet(onDismissRequest = onClose, containerColor = tint, contentColor = ink) {` · Por cruzar. [RecommendationsSheet.kt:51](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/RecommendationsSheet.kt#L51)
- `Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onPlay(song) }.padding(8.dp),` · Por cruzar. [RecommendationsSheet.kt:59](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/RecommendationsSheet.kt#L59)
- `IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options)) }` · Por cruzar. [RecommendationsSheet.kt:69](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/RecommendationsSheet.kt#L69)

</details>

<details>
<summary>feature/player/sheets/SleepTimerSheet.kt (8)</summary>

- `ModalBottomSheet(onDismissRequest = close, containerColor = Color(0xFF17151B)) {` · Por cruzar. [SleepTimerSheet.kt:33](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/SleepTimerSheet.kt#L33)
- `FilledTonalButton(onClick = { playback.extendSleepTimer(extra) }) {` · Por cruzar. [SleepTimerSheet.kt:55](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/SleepTimerSheet.kt#L55)
- `OutlinedButton(onClick = { playback.setSleepTimer(0) }) { Text(stringResource(R.string.off)) }` · Por cruzar. [SleepTimerSheet.kt:59](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/SleepTimerSheet.kt#L59)
- `Row(Modifier.fillMaxWidth().height(58.dp).clickable { customVisible = true }, verticalAlignment = Alignment.CenterVertically) {` · Por cruzar. [SleepTimerSheet.kt:68](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/SleepTimerSheet.kt#L68)
- `Row(Modifier.fillMaxWidth().height(58.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {` · Por cruzar. [SleepTimerSheet.kt:82](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/SleepTimerSheet.kt#L82)
- `Switch(value, onChange)` · Por cruzar. [SleepTimerSheet.kt:95](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/SleepTimerSheet.kt#L95)
- `IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, stringResource(R.string.back)) }` · Por cruzar. [SleepTimerSheet.kt:104](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/SleepTimerSheet.kt#L104)
- `Button(enabled = hours + minutes > 0, onClick = { onSet(hours * 60 + minutes) },` · Por cruzar. [SleepTimerSheet.kt:112](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/sheets/SleepTimerSheet.kt#L112)

</details>

<details>
<summary>feature/player/tags/AudioTagEditorSheet.kt (10)</summary>

- `val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->` · Por cruzar. [AudioTagEditorSheet.kt:69](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/AudioTagEditorSheet.kt#L69)
- `onBack = close, onSave = ::save) {` · Por cruzar. [AudioTagEditorSheet.kt:88](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/AudioTagEditorSheet.kt#L88)
- `onReplace = { tags = tags.copy(title = suggestion.first, artist = suggestion.second); suggestionDismissed = true },` · Por cruzar. [AudioTagEditorSheet.kt:93](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/AudioTagEditorSheet.kt#L93)
- `onDismiss = { suggestionDismissed = true })` · Por cruzar. [AudioTagEditorSheet.kt:94](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/AudioTagEditorSheet.kt#L94)
- `FilledTonalButton(onClick = { autoFill = true }, enabled = supported, modifier = Modifier.padding(top = 12.dp)) {` · Por cruzar. [AudioTagEditorSheet.kt:97](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/AudioTagEditorSheet.kt#L97)
- `if (autoFill) TagSuggestDialog(tags.title, tags.artist, tags.album, song.durationMs, onDismiss = { autoFill = false },` · Por cruzar. [AudioTagEditorSheet.kt:101](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/AudioTagEditorSheet.kt#L101)
- `onPick = { pick ->` · Por cruzar. [AudioTagEditorSheet.kt:102](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/AudioTagEditorSheet.kt#L102)
- `Box(Modifier.size(120.dp).clip(RoundedCornerShape(18.dp)).clickable(enabled = supported) { picker.launch("image/*") }) {` · Por cruzar. [AudioTagEditorSheet.kt:122](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/AudioTagEditorSheet.kt#L122)
- `FilledTonalButton(onClick = { picker.launch("image/*") }, enabled = supported) {` · Por cruzar. [AudioTagEditorSheet.kt:132](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/AudioTagEditorSheet.kt#L132)
- `OutlinedButton(onClick = { tags = tags.copy(cover = null, removeCover = true); preview = null }, enabled = supported) {` · Por cruzar. [AudioTagEditorSheet.kt:136](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/AudioTagEditorSheet.kt#L136)

</details>

<details>
<summary>feature/player/tags/MediaWriteGate.kt (1)</summary>

- `val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->` · Por cruzar. [MediaWriteGate.kt:22](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/MediaWriteGate.kt#L22)

</details>

<details>
<summary>feature/player/tags/TagEditorParts.kt (5)</summary>

- `Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false,` · Por cruzar. [TagEditorParts.kt:29](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/TagEditorParts.kt#L29)
- `IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBackIosNew, stringResource(R.string.back)) }` · Por cruzar. [TagEditorParts.kt:36](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/TagEditorParts.kt#L36)
- `Button(onClick = onSave, enabled = canSave && !busy, colors = ButtonDefaults.buttonColors(` · Por cruzar. [TagEditorParts.kt:38](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/TagEditorParts.kt#L38)
- `IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.close)) }` · Por cruzar. [TagEditorParts.kt:75](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/TagEditorParts.kt#L75)
- `TextButton(onClick = onReplace) { Text(stringResource(R.string.tags_replace), color = StonePalette.Blue, fontSize = 16.sp) }` · Por cruzar. [TagEditorParts.kt:82](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/TagEditorParts.kt#L82)

</details>

<details>
<summary>feature/player/tags/TagSuggestDialog.kt (3)</summary>

- `AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.tags_auto_title)) },` · Por cruzar. [TagSuggestDialog.kt:43](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/TagSuggestDialog.kt#L43)
- `Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(candidate) }.padding(vertical = 8.dp),` · Por cruzar. [TagSuggestDialog.kt:53](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/TagSuggestDialog.kt#L53)
- `confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })` · Por cruzar. [TagSuggestDialog.kt:69](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/TagSuggestDialog.kt#L69)

</details>

<details>
<summary>feature/player/tags/VideoTagEditorSheet.kt (6)</summary>

- `val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->` · Por cruzar. [VideoTagEditorSheet.kt:69](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/VideoTagEditorSheet.kt#L69)
- `TagEditorScreen(R.string.video_edit_title, canSave = draft != original, busy = busy, onBack = close, onSave = ::save) {` · Por cruzar. [VideoTagEditorSheet.kt:92](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/VideoTagEditorSheet.kt#L92)
- `.clickable { imagePicker.launch("image/*") }) {` · Por cruzar. [VideoTagEditorSheet.kt:94](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/VideoTagEditorSheet.kt#L94)
- `FilledTonalButton(onClick = { imagePicker.launch("image/*") }, enabled = !busy) {` · Por cruzar. [VideoTagEditorSheet.kt:105](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/VideoTagEditorSheet.kt#L105)
- `OutlinedButton(enabled = frame?.bitmap != null && !busy, onClick = {` · Por cruzar. [VideoTagEditorSheet.kt:109](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/VideoTagEditorSheet.kt#L109)
- `Row(Modifier.fillMaxWidth().padding(top = 18.dp).clickable { details = !details },` · Por cruzar. [VideoTagEditorSheet.kt:122](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/tags/VideoTagEditorSheet.kt#L122)

</details>

<details>
<summary>feature/player/video/LandscapeVideoChrome.kt (21)</summary>

- `Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) {})` · Por cruzar. [LandscapeVideoChrome.kt:74](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L74)
- `IconButton(onClick = { locked = false }, Modifier.align(Alignment.CenterStart).padding(start = 32.dp)` · Por cruzar. [LandscapeVideoChrome.kt:75](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L75)
- `IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBackIosNew, stringResource(R.string.close_player), tint = Color.White) }` · Por cruzar. [LandscapeVideoChrome.kt:86](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L86)
- `if (hasSubtitles) IconButton(onClick = {` · Por cruzar. [LandscapeVideoChrome.kt:91](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L91)
- `IconButton(onClick = { panel = VideoPanel.List }) {` · Por cruzar. [LandscapeVideoChrome.kt:96](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L96)
- `IconButton(onClick = { panel = VideoPanel.Menu }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options), tint = Color.White) }` · Por cruzar. [LandscapeVideoChrome.kt:99](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L99)
- `SideAction(Icons.Default.AspectRatio, R.string.video_size, labelFirst = false, onClick = onScaleToggle)` · Por cruzar. [LandscapeVideoChrome.kt:105](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L105)
- `SideAction(Icons.Default.Headphones, R.string.video_background_play, onClick = onAudioOnly)` · Por cruzar. [LandscapeVideoChrome.kt:112](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L112)
- `IconButton(onClick = { locked = true; panel = VideoPanel.None }) {` · Por cruzar. [LandscapeVideoChrome.kt:121](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L121)
- `IconButton(onClick = playback::previous) { Icon(Icons.Default.SkipPrevious, stringResource(R.string.previous), Modifier.size(34.dp), tint = Color.Whit` · Por cruzar. [LandscapeVideoChrome.kt:125](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L125)
- `IconButton(onClick = { seekVideoBy(playback, progress, -10_000L) }) { Icon(Icons.Default.Replay10, null, Modifier.size(30.dp), tint = Color.White) }` · Por cruzar. [LandscapeVideoChrome.kt:126](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L126)
- `IconButton(onClick = playback::toggle, Modifier.padding(horizontal = 14.dp).size(64.dp).border(3.dp, Color.White, CircleShape)) {` · Por cruzar. [LandscapeVideoChrome.kt:127](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L127)
- `IconButton(onClick = { seekVideoBy(playback, progress, 10_000L) }) { Icon(Icons.Default.Forward10, null, Modifier.size(30.dp), tint = Color.White) }` · Por cruzar. [LandscapeVideoChrome.kt:131](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L131)
- `IconButton(onClick = playback::next) { Icon(Icons.Default.SkipNext, stringResource(R.string.next), Modifier.size(34.dp), tint = Color.White) }` · Por cruzar. [LandscapeVideoChrome.kt:132](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L132)
- `TextButton(onClick = { onTool(PlayerTool.Speed) }) {` · Por cruzar. [LandscapeVideoChrome.kt:134](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L134)
- `IconButton(onClick = { enterVideoPictureInPicture(context, player) }) {` · Por cruzar. [LandscapeVideoChrome.kt:137](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L137)
- `VideoPanel.List -> VideoListPanel(state, playback, onClose = { panel = VideoPanel.None })` · Por cruzar. [LandscapeVideoChrome.kt:147](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L147)
- `else -> VideoMenuPanel(song, state, playback, night, onNight = { night = it },` · Por cruzar. [LandscapeVideoChrome.kt:148](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L148)
- `onTool = { panel = VideoPanel.None; onTool(it) }, onAudioOnly = { panel = VideoPanel.None; onAudioOnly() },` · Por cruzar. [LandscapeVideoChrome.kt:149](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L149)
- `onClose = { panel = VideoPanel.None })` · Por cruzar. [LandscapeVideoChrome.kt:150](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L150)
- `Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),` · Por cruzar. [LandscapeVideoChrome.kt:163](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoChrome.kt#L163)

</details>

<details>
<summary>feature/player/video/LandscapeVideoPreviewPanel.kt (21)</summary>

- `onSelectVideo = onSelectVideo,` · Por cruzar. [LandscapeVideoPreviewPanel.kt:78](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L78)
- `onOpenFullscreen = onOpenFullscreen,` · Por cruzar. [LandscapeVideoPreviewPanel.kt:79](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L79)
- `onAudioOnly = { onAudioOnly(song) },` · Por cruzar. [LandscapeVideoPreviewPanel.kt:80](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L80)
- `onOptions = onOptions,` · Por cruzar. [LandscapeVideoPreviewPanel.kt:81](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L81)
- `onQueue = onQueue,` · Por cruzar. [LandscapeVideoPreviewPanel.kt:82](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L82)
- `onScaleToggle = onScaleToggle,` · Por cruzar. [LandscapeVideoPreviewPanel.kt:83](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L83)
- `onCheckedChange = { if (active) playback.toggleFavorite() },` · Por cruzar. [LandscapeVideoPreviewPanel.kt:151](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L151)
- `IconButton(onClick = onOptions, enabled = active) {` · Por cruzar. [LandscapeVideoPreviewPanel.kt:160](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L160)
- `detectTapGestures(` · Por cruzar. [LandscapeVideoPreviewPanel.kt:202](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L202)
- `onTap = { controlsVisible = !controlsVisible },` · Por cruzar. [LandscapeVideoPreviewPanel.kt:203](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L203)
- `onDoubleTap = {` · Por cruzar. [LandscapeVideoPreviewPanel.kt:204](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L204)
- `onClick = { currentPlaybackToggle.value() },` · Por cruzar. [LandscapeVideoPreviewPanel.kt:213](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L213)
- `IconButton(onClick = playback::previous, enabled = active) {` · Por cruzar. [LandscapeVideoPreviewPanel.kt:252](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L252)
- `onClick = { currentPlaybackToggle.value() },` · Por cruzar. [LandscapeVideoPreviewPanel.kt:256](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L256)
- `IconButton(onClick = playback::next, enabled = active) {` · Por cruzar. [LandscapeVideoPreviewPanel.kt:270](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L270)
- `onAudioOnly = onAudioOnly,` · Por cruzar. [LandscapeVideoPreviewPanel.kt:277](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L277)
- `onOptions = onOptions,` · Por cruzar. [LandscapeVideoPreviewPanel.kt:278](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L278)
- `onQueue = onQueue,` · Por cruzar. [LandscapeVideoPreviewPanel.kt:279](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L279)
- `onScaleToggle = onScaleToggle,` · Por cruzar. [LandscapeVideoPreviewPanel.kt:280](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L280)
- `Slider(` · Por cruzar. [LandscapeVideoPreviewPanel.kt:299](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L299)
- `onValueChange = {},` · Por cruzar. [LandscapeVideoPreviewPanel.kt:301](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/LandscapeVideoPreviewPanel.kt#L301)

</details>

<details>
<summary>feature/player/video/PortraitVideoChrome.kt (14)</summary>

- `Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) {})` · Por cruzar. [PortraitVideoChrome.kt:55](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L55)
- `IconButton(onClick = { locked = false }, Modifier.align(Alignment.CenterStart).padding(start = 24.dp)` · Por cruzar. [PortraitVideoChrome.kt:56](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L56)
- `IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBackIosNew, stringResource(R.string.close_player), tint = Color.White) }` · Por cruzar. [PortraitVideoChrome.kt:67](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L67)
- `IconButton(onClick = onQueue) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, stringResource(R.string.queue), tint = Color.White) }` · Por cruzar. [PortraitVideoChrome.kt:69](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L69)
- `IconButton(onClick = onOptions) { Icon(Icons.Default.MoreVert, stringResource(R.string.more_options), tint = Color.White) }` · Por cruzar. [PortraitVideoChrome.kt:70](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L70)
- `VideoControlRow(carousel, Modifier.wrapContentWidth(), onAudioOnly = onAudioOnly, onScaleToggle = onScaleToggle,` · Por cruzar. [PortraitVideoChrome.kt:75](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L75)
- `onRotate = { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE },` · Por cruzar. [PortraitVideoChrome.kt:76](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L76)
- `.clickable(onClick = onSpeed).padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold)` · Por cruzar. [PortraitVideoChrome.kt:89](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L89)
- `IconButton(onClick = { enterVideoPictureInPicture(context, playback.player()) }) {` · Por cruzar. [PortraitVideoChrome.kt:93](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L93)
- `IconButton(onClick = playback::previous) { Icon(Icons.Default.SkipPrevious, stringResource(R.string.previous), Modifier.size(34.dp), tint = Color.Whit` · Por cruzar. [PortraitVideoChrome.kt:96](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L96)
- `IconButton(onClick = playback::toggle, Modifier.size(68.dp).border(3.dp, Color.White, CircleShape)) {` · Por cruzar. [PortraitVideoChrome.kt:97](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L97)
- `IconButton(onClick = playback::next) { Icon(Icons.Default.SkipNext, stringResource(R.string.next), Modifier.size(34.dp), tint = Color.White) }` · Por cruzar. [PortraitVideoChrome.kt:101](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L101)
- `IconButton(onClick = { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE }) {` · Por cruzar. [PortraitVideoChrome.kt:102](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L102)
- `FilledIconButton(onClick = onClick, modifier = Modifier.padding(horizontal = 6.dp).size(52.dp),` · Por cruzar. [PortraitVideoChrome.kt:113](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/PortraitVideoChrome.kt#L113)

</details>

<details>
<summary>feature/player/video/VideoControlRow.kt (2)</summary>

- `if (circle) FilledIconButton(onClick = it, modifier = Modifier.padding(horizontal = 6.dp).size(52.dp),` · Por cruzar. [VideoControlRow.kt:104](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoControlRow.kt#L104)
- `} else IconButton(onClick = it, modifier = Modifier.size(48.dp)) {` · Por cruzar. [VideoControlRow.kt:107](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoControlRow.kt#L107)

</details>

<details>
<summary>feature/player/video/VideoGestures.kt (7)</summary>

- `detectVideoGestures(` · stone.video.gestures_and_pip. [VideoGestures.kt:90](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoGestures.kt#L90)
- `onTap = { currentTap.value() },` · Por cruzar. [VideoGestures.kt:91](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoGestures.kt#L91)
- `onDoubleTap = { currentDoubleTap.value(it) },` · Por cruzar. [VideoGestures.kt:92](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoGestures.kt#L92)
- `onVerticalChange = { leftSide, delta -> currentAdjustment.value(leftSide, delta) },` · Por cruzar. [VideoGestures.kt:93](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoGestures.kt#L93)
- `onHorizontalSwipe = { deltaX ->` · Por cruzar. [VideoGestures.kt:94](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoGestures.kt#L94)
- `onHold = { start -> currentHold.value(start) }` · Por cruzar. [VideoGestures.kt:97](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoGestures.kt#L97)
- `internal suspend fun PointerInputScope.detectVideoGestures(onTap: () -> Unit,` · Por cruzar. [VideoGestures.kt:143](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoGestures.kt#L143)

</details>

<details>
<summary>feature/player/video/VideoOptionsSheet.kt (11)</summary>

- `onPickTrack = { picker = it })` · Por cruzar. [VideoOptionsSheet.kt:54](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoOptionsSheet.kt#L54)
- `Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false,` · Por cruzar. [VideoOptionsSheet.kt:57](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoOptionsSheet.kt#L57)
- `Box(Modifier.weight(1f).fillMaxHeight().clickable(` · Por cruzar. [VideoOptionsSheet.kt:61](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoOptionsSheet.kt#L61)
- `interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),` · Por cruzar. [VideoOptionsSheet.kt:62](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoOptionsSheet.kt#L62)
- `ModalBottomSheet(onDismissRequest = onClose, containerColor = PanelColor) {` · Por cruzar. [VideoOptionsSheet.kt:71](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoOptionsSheet.kt#L71)
- `IconButton(onClick = { onSelect(PlayerTool.Share) }) {` · Por cruzar. [VideoOptionsSheet.kt:96](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoOptionsSheet.kt#L96)
- `Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),` · Por cruzar. [VideoOptionsSheet.kt:131](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoOptionsSheet.kt#L131)
- `AlertDialog(onDismissRequest = close, containerColor = PanelColor,` · Por cruzar. [VideoOptionsSheet.kt:169](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoOptionsSheet.kt#L169)
- `if (type == C.TRACK_TYPE_TEXT) TextButton(onClick = {` · Por cruzar. [VideoOptionsSheet.kt:173](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoOptionsSheet.kt#L173)
- `TextButton(onClick = {` · Por cruzar. [VideoOptionsSheet.kt:185](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoOptionsSheet.kt#L185)
- `confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } })` · Por cruzar. [VideoOptionsSheet.kt:198](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoOptionsSheet.kt#L198)

</details>

<details>
<summary>feature/player/video/VideoPlayers.kt (10)</summary>

- `onDoubleTap = { leftSide ->` · Por cruzar. [VideoPlayers.kt:79](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoPlayers.kt#L79)
- `}, onPrevious = playback::previous, onNext = playback::next,` · Por cruzar. [VideoPlayers.kt:81](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoPlayers.kt#L81)
- `LandscapeVideoChrome(song, state, progress, playback, controls.visible, onBack = close,` · Por cruzar. [VideoPlayers.kt:83](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoPlayers.kt#L83)
- `onAudioOnly = { onAudioOnly(song) }, onScaleToggle = { onVideoScaleChange(videoScale.next()) },` · Por cruzar. [VideoPlayers.kt:84](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoPlayers.kt#L84)
- `onTool = onTool)` · Por cruzar. [VideoPlayers.kt:85](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoPlayers.kt#L85)
- `onDoubleTap = { leftSide ->` · Por cruzar. [VideoPlayers.kt:107](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoPlayers.kt#L107)
- `}, onPrevious = playback::previous, onNext = playback::next,` · Por cruzar. [VideoPlayers.kt:109](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoPlayers.kt#L109)
- `PortraitVideoChrome(song, state, progress, playback, controls.visible, videoControls, onBack = back,` · Por cruzar. [VideoPlayers.kt:111](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoPlayers.kt#L111)
- `onQueue = onQueue, onOptions = onOptions, onAudioOnly = { onAudioOnly(song) },` · Por cruzar. [VideoPlayers.kt:112](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoPlayers.kt#L112)
- `onScaleToggle = { onVideoScaleChange(videoScale.next()) }, onSpeed = onSpeed)` · Por cruzar. [VideoPlayers.kt:113](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoPlayers.kt#L113)

</details>

<details>
<summary>feature/player/video/VideoSidePanels.kt (8)</summary>

- `Slider(value, { value = it; setWindowBrightness(context, it) }, Modifier.padding(horizontal = 24.dp), valueRange = .02f..1f)` · Por cruzar. [VideoSidePanels.kt:66](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoSidePanels.kt#L66)
- `if (expanded == "volume") Slider(state.volumePercent.toFloat(), { StonePlaybackService.setVolumePercent(it.roundToInt()) },` · stone.player.set_volume. [VideoSidePanels.kt:69](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoSidePanels.kt#L69)
- `MenuRow(Icons.Default.MusicNote, R.string.audio_only, onClick = onAudioOnly)` · Por cruzar. [VideoSidePanels.kt:72](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoSidePanels.kt#L72)
- `AlertDialog(onDismissRequest = { properties = false; onClose() },` · Por cruzar. [VideoSidePanels.kt:81](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoSidePanels.kt#L81)
- `confirmButton = { TextButton(onClick = { properties = false }) { Text(stringResource(R.string.close)) } })` · Por cruzar. [VideoSidePanels.kt:93](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoSidePanels.kt#L93)
- `Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),` · Por cruzar. [VideoSidePanels.kt:98](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoSidePanels.kt#L98)
- `IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBackIosNew, stringResource(R.string.back), tint = Color.White) }` · Por cruzar. [VideoSidePanels.kt:118](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoSidePanels.kt#L118)
- `.clickable { playback.playAt(index) }.padding(horizontal = 16.dp, vertical = 8.dp),` · Por cruzar. [VideoSidePanels.kt:126](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoSidePanels.kt#L126)

</details>

<details>
<summary>feature/player/video/VideoTimeline.kt (1)</summary>

- `PlayerProgress(progress, playback, onVerticalDrag = { dragAmount ->` · Por cruzar. [VideoTimeline.kt:38](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/player/video/VideoTimeline.kt#L38)

</details>

<details>
<summary>feature/settings/AudioLevelSettings.kt (3)</summary>

- `IconButton(onClick = { AudioLevels.reset(store); revision++; active?.let { playback?.setAudioMode(it) } }) {` · Por cruzar. [AudioLevelSettings.kt:56](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/AudioLevelSettings.kt#L56)
- `.clickable { selected = card.mode; playback?.setAudioMode(card.mode) }.padding(16.dp)) {` · Por cruzar. [AudioLevelSettings.kt:68](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/AudioLevelSettings.kt#L68)
- `Slider(value.toFloat(), change, valueRange = range, onValueChangeFinished = done)` · Por cruzar. [AudioLevelSettings.kt:113](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/AudioLevelSettings.kt#L113)

</details>

<details>
<summary>feature/settings/LibraryScanSettings.kt (3)</summary>

- `val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {` · Por cruzar. [LibraryScanSettings.kt:29](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/LibraryScanSettings.kt#L29)
- `OutlinedButton(onClick = { request.launch(DeviceMediaAccess.permissions()) },` · Por cruzar. [LibraryScanSettings.kt:50](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/LibraryScanSettings.kt#L50)
- `OutlinedButton(onClick = {` · Por cruzar. [LibraryScanSettings.kt:55](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/LibraryScanSettings.kt#L55)

</details>

<details>
<summary>feature/settings/McpByArka.kt (1)</summary>

- `Button(onClick = {` · Por cruzar. [McpByArka.kt:39](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/McpByArka.kt#L39)

</details>

<details>
<summary>feature/settings/NotificationSettings.kt (7)</summary>

- `val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->` · Por cruzar. [NotificationSettings.kt:40](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/NotificationSettings.kt#L40)
- `AlertDialog(onDismissRequest = { picking = null },` · Por cruzar. [NotificationSettings.kt:75](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/NotificationSettings.kt#L75)
- `confirmButton = { TextButton(onClick = {` · stone.notifications.schedule. [NotificationSettings.kt:76](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/NotificationSettings.kt#L76)
- `dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.cancel)) } },` · Por cruzar. [NotificationSettings.kt:82](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/NotificationSettings.kt#L82)
- `Row(Modifier.fillMaxWidth().clickable { change(!checked) }, verticalAlignment = Alignment.CenterVertically) {` · Por cruzar. [NotificationSettings.kt:91](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/NotificationSettings.kt#L91)
- `Row(Modifier.fillMaxWidth().clickable(onClick = select), verticalAlignment = Alignment.CenterVertically) {` · Por cruzar. [NotificationSettings.kt:97](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/NotificationSettings.kt#L97)
- `Row(Modifier.fillMaxWidth().clickable(onClick = open).padding(vertical = 10.dp, horizontal = 12.dp),` · Por cruzar. [NotificationSettings.kt:103](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/NotificationSettings.kt#L103)

</details>

<details>
<summary>feature/settings/SettingsParts.kt (2)</summary>

- `Row(Modifier.fillMaxWidth().height(58.dp).clickable(onClick = action), verticalAlignment = Alignment.CenterVertically) {` · Por cruzar. [SettingsParts.kt:22](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsParts.kt#L22)
- `Switch(value, change)` · Por cruzar. [SettingsParts.kt:39](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsParts.kt#L39)

</details>

<details>
<summary>feature/settings/SettingsPlaybackDetails.kt (5)</summary>

- `Button(onClick = {` · Por cruzar. [SettingsPlaybackDetails.kt:52](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsPlaybackDetails.kt#L52)
- `Row(Modifier.fillMaxWidth().heightIn(min = 82.dp).clickable(onClick = open),` · Por cruzar. [SettingsPlaybackDetails.kt:90](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsPlaybackDetails.kt#L90)
- `Row(Modifier.fillMaxWidth().heightIn(min = 82.dp).clickable(onClick = open),` · Por cruzar. [SettingsPlaybackDetails.kt:109](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsPlaybackDetails.kt#L109)
- `Row(Modifier.fillMaxWidth().clickable {` · Por cruzar. [SettingsPlaybackDetails.kt:138](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsPlaybackDetails.kt#L138)
- `Row(Modifier.fillMaxWidth().clickable { playback?.setHardwareAcceleration(mode) }` · Por cruzar. [SettingsPlaybackDetails.kt:168](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsPlaybackDetails.kt#L168)

</details>

<details>
<summary>feature/settings/SettingsScreens.kt (8)</summary>

- `ModalBottomSheet(onDismissRequest = { if (page != null) onPage(if (page in legalPages) "about" else null) else onClose() },` · Por cruzar. [SettingsScreens.kt:60](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsScreens.kt#L60)
- `BackHandler(enabled = page != null, onBack = { onPage(if (page in legalPages) "about" else null) })` · Por cruzar. [SettingsScreens.kt:65](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsScreens.kt#L65)
- `OutlinedTextField(value = settingsQuery, onValueChange = { settingsQuery = it.take(60) },` · stone.navigation.open_settings. [SettingsScreens.kt:70](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsScreens.kt#L70)
- `if (settingsQuery.isNotEmpty()) IconButton(onClick = { settingsQuery = "" }) {` · Por cruzar. [SettingsScreens.kt:75](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsScreens.kt#L75)
- `IconButton(onClick = { onPage(if (page in legalPages) "about" else null) }) {` · Por cruzar. [SettingsScreens.kt:150](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsScreens.kt#L150)
- `"sleep" -> SleepDetail(playback, onClose = { onPage(null) })` · stone.player.sleep_timer. [SettingsScreens.kt:189](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsScreens.kt#L189)
- `Row(Modifier.fillMaxWidth().height(66.dp).clickable(onClick = action), verticalAlignment = Alignment.CenterVertically) {` · Por cruzar. [SettingsScreens.kt:229](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsScreens.kt#L229)
- `Row(Modifier.fillMaxWidth().height(66.dp).clickable(onClick = item.action),` · Por cruzar. [SettingsScreens.kt:239](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsScreens.kt#L239)

</details>

<details>
<summary>feature/settings/SettingsShortcutsDetail.kt (3)</summary>

- `FilterChip(selected = store?.shortcutProfile() == id, onClick = {` · Por cruzar. [SettingsShortcutsDetail.kt:68](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsShortcutsDetail.kt#L68)
- `OutlinedButton(onClick = {` · Por cruzar. [SettingsShortcutsDetail.kt:92](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsShortcutsDetail.kt#L92)
- `Switch(checked = visible[key] == true, enabled = item.available, onCheckedChange = { enabled ->` · Por cruzar. [SettingsShortcutsDetail.kt:126](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SettingsShortcutsDetail.kt#L126)

</details>

<details>
<summary>feature/settings/SystemDiagnosticsDetail.kt (9)</summary>

- `Button(enabled = !running, onClick = { snapshot = PerformanceDiagnostics.read(context) },` · Por cruzar. [SystemDiagnosticsDetail.kt:82](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SystemDiagnosticsDetail.kt#L82)
- `OutlinedButton(enabled = !running, onClick = {` · Por cruzar. [SystemDiagnosticsDetail.kt:84](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SystemDiagnosticsDetail.kt#L84)
- `FilterChip(selected = duration == value, onClick = { duration = value }, label = { Text(label) })` · Por cruzar. [SystemDiagnosticsDetail.kt:98](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SystemDiagnosticsDetail.kt#L98)
- `OutlinedButton(onClick = { com.zirakusa.stone.nativeapp.playback.diagnostics.DiagnosticsReportExporter.share(context, result) }, modifier = Modifier.f` · Por cruzar. [SystemDiagnosticsDetail.kt:102](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SystemDiagnosticsDetail.kt#L102)
- `Switch(checked = audioDiagnostic.loudnessEnhancerDisabled,` · Por cruzar. [SystemDiagnosticsDetail.kt:122](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SystemDiagnosticsDetail.kt#L122)
- `onCheckedChange = StonePlaybackService::setDiagnosticBoostDisabled)` · Por cruzar. [SystemDiagnosticsDetail.kt:123](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SystemDiagnosticsDetail.kt#L123)
- `OutlinedButton(onClick = {` · Por cruzar. [SystemDiagnosticsDetail.kt:127](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SystemDiagnosticsDetail.kt#L127)
- `OutlinedButton(onClick = {` · Por cruzar. [SystemDiagnosticsDetail.kt:133](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SystemDiagnosticsDetail.kt#L133)
- `TextButton(onClick = { open = !open }, modifier = Modifier.fillMaxWidth()) {` · Por cruzar. [SystemDiagnosticsDetail.kt:206](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/SystemDiagnosticsDetail.kt#L206)

</details>

<details>
<summary>feature/settings/TransitionSettings.kt (2)</summary>

- `Slider(seconds.toFloat(), { seconds = it.roundToInt().coerceIn(1, 12) }, Modifier.weight(1f),` · Por cruzar. [TransitionSettings.kt:33](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/TransitionSettings.kt#L33)
- `onValueChangeFinished = { store?.setCrossfadeSeconds(seconds); apply() })` · Por cruzar. [TransitionSettings.kt:35](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/TransitionSettings.kt#L35)

</details>

<details>
<summary>feature/settings/VideoControlSettings.kt (2)</summary>

- `onClick = {` · Por cruzar. [VideoControlSettings.kt:34](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/VideoControlSettings.kt#L34)
- `Switch(checked = visible[id] == true, onCheckedChange = { enabled ->` · stone.video.gestures_and_pip. [VideoControlSettings.kt:72](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/VideoControlSettings.kt#L72)

</details>

<details>
<summary>feature/settings/WidgetSettings.kt (1)</summary>

- `Button(onClick = {` · stone.settings.pin_widget. [WidgetSettings.kt:51](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/WidgetSettings.kt#L51)

</details>

<details>
<summary>feature/settings/feedback/CrashReportCard.kt (3)</summary>

- `FilledTonalButton(onClick = {` · Por cruzar. [CrashReportCard.kt:30](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/CrashReportCard.kt#L30)
- `FilledTonalButton(onClick = {` · Por cruzar. [CrashReportCard.kt:34](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/CrashReportCard.kt#L34)
- `TextButton(onClick = { CrashLog.clear(context); report = null }) { Text(stringResource(R.string.delete)) }` · Por cruzar. [CrashReportCard.kt:38](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/CrashReportCard.kt#L38)

</details>

<details>
<summary>feature/settings/feedback/FeedbackFormParts.kt (5)</summary>

- `.clickable { onSelect(kind) }.padding(horizontal = 8.dp, vertical = 8.dp),` · Por cruzar. [FeedbackFormParts.kt:58](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackFormParts.kt#L58)
- `Row(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)` · Por cruzar. [FeedbackFormParts.kt:79](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackFormParts.kt#L79)
- `IconButton(onClick = { remove(uri) }, modifier = Modifier.align(Alignment.TopEnd)` · Por cruzar. [FeedbackFormParts.kt:97](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackFormParts.kt#L97)
- `Checkbox(checked = checked, onCheckedChange = onChange)` · Por cruzar. [FeedbackFormParts.kt:110](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackFormParts.kt#L110)
- `IconButton(onClick = onInfo) { Icon(Icons.Outlined.Info, stringResource(R.string.feedback_review_before_share),` · Por cruzar. [FeedbackFormParts.kt:113](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackFormParts.kt#L113)

</details>

<details>
<summary>feature/settings/feedback/FeedbackReport.kt (7)</summary>

- `Switch(shakeToReport, onShakeToReportChange)` · Por cruzar. [FeedbackReport.kt:52](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackReport.kt#L52)
- `ModalBottomSheet(onDismissRequest = close, containerColor = StonePalette.Surface,` · Por cruzar. [FeedbackReport.kt:64](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackReport.kt#L64)
- `FeedbackForm(initialKind, screenLabel, onShared = close, initialAttachments = initialAttachments,` · Por cruzar. [FeedbackReport.kt:67](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackReport.kt#L67)
- `val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selected ->` · Por cruzar. [FeedbackReport.kt:82](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackReport.kt#L82)
- `FilterChip(selected = category == option, onClick = { category = option },` · Por cruzar. [FeedbackReport.kt:101](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackReport.kt#L101)
- `OutlinedTextField(value = details, onValueChange = { details = it.take(MAX_DETAILS) },` · Por cruzar. [FeedbackReport.kt:107](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackReport.kt#L107)
- `Button(onClick = {` · Por cruzar. [FeedbackReport.kt:119](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/feedback/FeedbackReport.kt#L119)

</details>

<details>
<summary>feature/settings/look/LookArtworkShape.kt (1)</summary>

- `Row(Modifier.fillMaxWidth().clickable { StoneLook.artworkShape = key; persist() }.padding(vertical = 6.dp),` · Por cruzar. [LookArtworkShape.kt:32](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookArtworkShape.kt#L32)

</details>

<details>
<summary>feature/settings/look/LookBackgroundEditor.kt (1)</summary>

- `Slider(StoneLook.backgroundOpacity, { StoneLook.backgroundOpacity = it }, valueRange = 0.1f..0.9f, onValueChangeFinished = persist)` · Por cruzar. [LookBackgroundEditor.kt:64](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookBackgroundEditor.kt#L64)

</details>

<details>
<summary>feature/settings/look/LookColorEditor.kt (1)</summary>

- `Slider(value, onChange, Modifier.weight(1f), valueRange = range)` · Por cruzar. [LookColorEditor.kt:55](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookColorEditor.kt#L55)

</details>

<details>
<summary>feature/settings/look/LookColorPalette.kt (2)</summary>

- `.pointerInput(Unit) { detectTapGestures { onPick(colorAt(it, size)) } }` · Por cruzar. [LookColorPalette.kt:39](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookColorPalette.kt#L39)
- `.pointerInput(Unit) { detectDragGestures { change, _ -> onPick(colorAt(change.position, size)) } }) {` · Por cruzar. [LookColorPalette.kt:40](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookColorPalette.kt#L40)

</details>

<details>
<summary>feature/settings/look/LookSettings.kt (4)</summary>

- `val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->` · stone.appearance.set_background_image. [LookSettings.kt:36](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookSettings.kt#L36)
- `val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->` · stone.appearance.set_avatar_image. [LookSettings.kt:40](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookSettings.kt#L40)
- `Button(onClick = { if (!pinShortcut(context)) Toast.makeText(context, R.string.custom_shortcut_unsupported,` · stone.appearance.pin_custom_shortcut. [LookSettings.kt:97](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookSettings.kt#L97)
- `OutlinedButton(onClick = { store?.let(StoneLook::reset) }, Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp)) {` · stone.appearance.reset_look. [LookSettings.kt:104](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookSettings.kt#L104)

</details>

<details>
<summary>feature/settings/look/LookSettingsParts.kt (5)</summary>

- `Column(Modifier.width(64.dp).clickable { onSelect(index) }, horizontalAlignment = Alignment.CenterHorizontally) {` · Por cruzar. [LookSettingsParts.kt:41](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookSettingsParts.kt#L41)
- `Slider(value, { onChange((it * 20).roundToInt() / 20f) }, Modifier.weight(1f), valueRange = range)` · Por cruzar. [LookSettingsParts.kt:60](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookSettingsParts.kt#L60)
- `FilledTonalButton(onClick = pick) { Text(stringResource(R.string.custom_pick_image)) }` · Por cruzar. [LookSettingsParts.kt:67](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookSettingsParts.kt#L67)
- `if (hasImage) OutlinedButton(onClick = remove) { Text(stringResource(R.string.custom_remove_image)) }` · Por cruzar. [LookSettingsParts.kt:68](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookSettingsParts.kt#L68)
- `Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onSelect(mascot) },` · Por cruzar. [LookSettingsParts.kt:77](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/LookSettingsParts.kt#L77)

</details>

<details>
<summary>feature/settings/look/ThemeExtras.kt (1)</summary>

- `Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp).clickable { onTheme(spec.key) }) {` · Por cruzar. [ThemeExtras.kt:35](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/feature/settings/look/ThemeExtras.kt#L35)

</details>

<details>
<summary>playback/StonePlaybackService.kt (1)</summary>

- `val openApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),` · stone.navigation.open_notification. [StonePlaybackService.kt:419](https://github.com/SiriusMagic/Stone/blob/cc06bf2afd5289e3f3d411411647b74084c97ab4/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/playback/StonePlaybackService.kt#L419)

</details>

<details>
<summary>playback/StoneWidgetProvider.kt (8)</summary>

- `setOnClickPendingIntent(R.id.widget_play, action(app, ACTION_PLAY, 1))` · stone.player.pause. [StoneWidgetProvider.kt:80](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/playback/StoneWidgetProvider.kt#L80)
- `setOnClickPendingIntent(R.id.widget_next, action(app, ACTION_NEXT, 2))` · stone.player.next. [StoneWidgetProvider.kt:81](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/playback/StoneWidgetProvider.kt#L81)
- `setOnClickPendingIntent(R.id.widget_prev, action(app, ACTION_PREVIOUS, 3))` · stone.player.previous. [StoneWidgetProvider.kt:82](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/playback/StoneWidgetProvider.kt#L82)
- `setOnClickPendingIntent(R.id.widget_favorite, action(app, ACTION_FAVORITE, 4))` · stone.library.set_favorite. [StoneWidgetProvider.kt:83](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/playback/StoneWidgetProvider.kt#L83)
- `setOnClickPendingIntent(R.id.widget_cover, openIntent(app))` · stone.navigation.open_player. [StoneWidgetProvider.kt:84](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/playback/StoneWidgetProvider.kt#L84)
- `setOnClickPendingIntent(R.id.widget_title, openIntent(app))` · stone.navigation.open_player. [StoneWidgetProvider.kt:85](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/playback/StoneWidgetProvider.kt#L85)
- `private fun action(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(context, code,` · Por cruzar. [StoneWidgetProvider.kt:91](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/playback/StoneWidgetProvider.kt#L91)
- `private fun openIntent(context: Context) = PendingIntent.getActivity(context, 10,` · stone.navigation.open_notification. [StoneWidgetProvider.kt:94](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/playback/StoneWidgetProvider.kt#L94)

</details>

<details>
<summary>ui/AutomationBridge.kt (4)</summary>

- `val writeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->` · Por cruzar. [AutomationBridge.kt:144](https://github.com/SiriusMagic/Stone/blob/cc06bf2afd5289e3f3d411411647b74084c97ab4/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/AutomationBridge.kt#L144)
- `AlertDialog(onDismissRequest = { StoneAutomation.answer(request, false) },` · Por cruzar. [AutomationBridge.kt:155](https://github.com/SiriusMagic/Stone/blob/cc06bf2afd5289e3f3d411411647b74084c97ab4/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/AutomationBridge.kt#L155)
- `confirmButton = { TextButton(onClick = { StoneAutomation.answer(request, true) }) {` · Por cruzar. [AutomationBridge.kt:158](https://github.com/SiriusMagic/Stone/blob/cc06bf2afd5289e3f3d411411647b74084c97ab4/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/AutomationBridge.kt#L158)
- `dismissButton = { TextButton(onClick = { StoneAutomation.answer(request, false) }) {` · Por cruzar. [AutomationBridge.kt:160](https://github.com/SiriusMagic/Stone/blob/cc06bf2afd5289e3f3d411411647b74084c97ab4/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/AutomationBridge.kt#L160)

</details>

<details>
<summary>ui/StoneApp.kt (43)</summary>

- `onTheme = { theme = it; settingsStore.setTheme(it) },` · stone.appearance.set_theme. [StoneApp.kt:95](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L95)
- `onLanguage = { language = it; settingsStore.setLanguage(it); (context as? Activity)?.recreate() },` · Por cruzar. [StoneApp.kt:96](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L96)
- `onVideoScale = { videoScale = VideoScale.fromKey(it); settingsStore.setVideoScale(videoScale.key) })` · Por cruzar. [StoneApp.kt:98](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L98)
- `val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {` · stone.library.pick_folder. [StoneApp.kt:156](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L156)
- `val detector = if (shakeToReport && sensor != null) ShakeDetector(onShake = {` · stone.settings.diagnostics_and_feedback. [StoneApp.kt:178](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L178)
- `onArtist = {` · Por cruzar. [StoneApp.kt:254](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L254)
- `}, onRecommendations = { recommendationsOpen = true }, isPictureInPicture = isPictureInPicture, onAudioOnly = { song ->` · stone.player.open_recommendations. [StoneApp.kt:257](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L257)
- `onVideoScaleChange = {` · stone.video.set_scale. [StoneApp.kt:263](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L263)
- `onDeletePermanently = library::deletePermanently,` · Por cruzar. [StoneApp.kt:271](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L271)
- `onMarkPurged = library::markPurged,` · Por cruzar. [StoneApp.kt:272](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L272)
- `onAudioOnly = { song ->` · Por cruzar. [StoneApp.kt:277](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L277)
- `onVideoOptions = { selected ->` · Por cruzar. [StoneApp.kt:282](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L282)
- `onSetPlaylistCover = library::setPlaylistCoverFromUri,` · Por cruzar. [StoneApp.kt:287](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L287)
- `onRestorePlaylistCover = library::restorePlaylistCover,` · Por cruzar. [StoneApp.kt:288](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L288)
- `onShakeToReportChange = {` · Por cruzar. [StoneApp.kt:290](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L290)
- `onOpenSettings = { settingsPage = null }, theme = theme,` · Por cruzar. [StoneApp.kt:294](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L294)
- `onThemeChange = { theme = it; settingsStore.setTheme(it); (context as? Activity)?.recreate() }, language = language,` · Por cruzar. [StoneApp.kt:295](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L295)
- `onLanguageChange = { language = it; settingsStore.setLanguage(it); (context as? Activity)?.recreate() },` · Por cruzar. [StoneApp.kt:296](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L296)
- `onSettingsPage = { settingsPage = it }, playbackViewModel = playback,` · Por cruzar. [StoneApp.kt:298](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L298)
- `settingsSheetVisible = settingsVisible, onSettingsSheetVisible = {` · Por cruzar. [StoneApp.kt:299](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L299)
- `onArtistDestinationConsumed = { artistDestination = null },` · Por cruzar. [StoneApp.kt:303](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L303)
- `onDelete = library::delete,` · stone.library.delete_file. [StoneApp.kt:304](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L304)
- `onLandscapeLayoutChange = {` · Por cruzar. [StoneApp.kt:306](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L306)
- `onVideoScaleChange = {` · Por cruzar. [StoneApp.kt:313](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L313)
- `onVideoControlChange = { videoControlRevision++ },` · Por cruzar. [StoneApp.kt:317](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L317)
- `onVideoLibraryLayoutChange = {` · Por cruzar. [StoneApp.kt:319](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L319)
- `onImmersiveEnabledChange = {` · Por cruzar. [StoneApp.kt:324](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L324)
- `onHome = { playerOpen = false },` · stone.navigation.open_library. [StoneApp.kt:328](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L328)
- `onEqualizer = { playerSheets.tool = PlayerTool.Equalizer },` · Por cruzar. [StoneApp.kt:329](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L329)
- `onOpen = if (landscapeMode == LandscapeLayoutMode.LIBRARY_ONLY ||` · Por cruzar. [StoneApp.kt:332](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L332)
- `onMetadataChanged = { library.refresh() },` · Por cruzar. [StoneApp.kt:336](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L336)
- `onSelectVideo = { selected -> playSong(selected, videoSongs) },` · Por cruzar. [StoneApp.kt:341](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L341)
- `onAudioOnly = { song ->` · Por cruzar. [StoneApp.kt:342](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L342)
- `onVideoScaleChange = {` · Por cruzar. [StoneApp.kt:348](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L348)
- `onPlay = { song -> playback.play(song, picks); recommendationsOpen = false },` · Por cruzar. [StoneApp.kt:362](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L362)
- `onPlayNext = { song -> playback.playNext(listOf(song)) },` · Por cruzar. [StoneApp.kt:363](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L363)
- `onClose = { recommendationsOpen = false })` · Por cruzar. [StoneApp.kt:364](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L364)
- `onPlayMode = { cyclePlayMode(context, player, playback) },` · Por cruzar. [StoneApp.kt:378](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L378)
- `onSwipeUp = { recommendationsOpen = true },` · Por cruzar. [StoneApp.kt:379](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L379)
- `onClose = closeMiniOptions,` · Por cruzar. [StoneApp.kt:402](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L402)
- `onSelect = { selected ->` · Por cruzar. [StoneApp.kt:403](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L403)
- `VolumeOverlay(volumeUi, onDismiss = { StonePlaybackService.hideVolumeOverlay(volumeUi.revision) })` · Por cruzar. [StoneApp.kt:446](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L446)
- `onLanguage = { language = it; settingsStore.setLanguage(it); (context as? Activity)?.recreate() }) {` · Por cruzar. [StoneApp.kt:449](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/StoneApp.kt#L449)

</details>

<details>
<summary>ui/components/SleepTimerIndicator.kt (1)</summary>

- `.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),` · Por cruzar. [SleepTimerIndicator.kt:58](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/components/SleepTimerIndicator.kt#L58)

</details>

<details>
<summary>ui/components/StoneIcon.kt (2)</summary>

- `Box(modifier.size(44.dp).clickable(enabled = enabled, role = Role.Button, onClickLabel = label,` · Por cruzar. [StoneIcon.kt:46](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/components/StoneIcon.kt#L46)
- `onClick = onClick).then(Modifier.semantics(mergeDescendants = true) {` · Por cruzar. [StoneIcon.kt:47](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/components/StoneIcon.kt#L47)

</details>

<details>
<summary>ui/system/VolumeOverlay.kt (4)</summary>

- `detectVerticalDragGestures(onDragStart = { travelled = 0f; closed = false }) { _, amount ->` · Por cruzar. [VolumeOverlay.kt:55](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/system/VolumeOverlay.kt#L55)
- `detectTapGestures { StonePlaybackService.setVolumePercent(at(it.x)) }` · Por cruzar. [VolumeOverlay.kt:83](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/system/VolumeOverlay.kt#L83)
- `detectHorizontalDragGestures(onDragEnd = { dragging = null }, onDragCancel = { dragging = null }) { change, _ ->` · Por cruzar. [VolumeOverlay.kt:86](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/system/VolumeOverlay.kt#L86)
- `.clickable { StonePlaybackService.setVolumePercent(level) })` · Por cruzar. [VolumeOverlay.kt:101](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/system/VolumeOverlay.kt#L101)

</details>

<details>
<summary>ui/theme/StoneTheme.kt (3)</summary>

- `primary = accent, onPrimary = Color(0xFF0A0A0E),` · Por cruzar. [StoneTheme.kt:83](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/theme/StoneTheme.kt#L83)
- `onBackground = StonePalette.Text, onSurface = StonePalette.Text,` · Por cruzar. [StoneTheme.kt:87](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/theme/StoneTheme.kt#L87)
- `onSurfaceVariant = Color(0xFFA0A0A5), error = Color(0xFFFF4C4C),` · Por cruzar. [StoneTheme.kt:88](https://github.com/SiriusMagic/Stone/blob/cf2f0951b469294c87d2108a71aa5440f1ddf498/android-native/app/src/main/java/com/zirakusa/stone/nativeapp/ui/theme/StoneTheme.kt#L88)

</details>
