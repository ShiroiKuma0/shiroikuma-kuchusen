# 12.4.7

* in Remote tab in SearchScreen, on popup of "Reserve all"
	* the search string plus searcher names are suggested as the feed name
	* added option of feed type and hasVideo
	* fixed crash on confirm
* in FeedSettings
	* added setting (with note) for feed type if it is synthetic, which activates proper settings for audio/video prefs
	* enabled audio/video qualities settings for synthetic feed
* added note in creating synthetic feed
* top searchbars are set for single line
* static circular progress indicators are set to animate
* some code refactoring

# 12.4.6

* in Remote tab in SearchScreen
	* added searching indicator on infobar
	* in menu, added "Reserve all" (shown only when search is complete) to save all the media to a synthetic feed
* when playing a remote media, it's automatically saved in synthetic feed "Remote history"
	* applicable to media in Remote tab in SearchScreen and Episodes mode of OnlineFeed screen
* untoast "set player buffer"

# 12.4.5

* likely fixed possible run time error when changing player buffer

# 12.4.4

* improved player buffer and made it dynamically set based on play speed
* one media list in Remote tab in SearchScreen is cached, re-search is not needed when come back

# 12.4.3

* ensure player position functions when playing from Remote tab of SearchScreen or Episodes mode of OnlineFeed screen
* added global qualities settings for audio and video (if any installed external app supports) in Settings->Playback

# 12.4.2

* in Remote tab of SearchScreen, disabled swipe actions (not applicable)
* Amended create/rename synthetic feed
	* name entry field is limited to one line
	* created feed is set to the current volume
* limit name entry field to one line in create/edit volume
* fixed possible crash when shelve episodes to a synthetic feed
* added note on selecting proper feed in add-to-synthetic-feed dialog

# 12.4.1

* PodciniLib upped to 1.1.2, external apps (if used) need to be updated for compatibility
* fixed video not showing from Remote tab of SearchScreen
* when tap on Pause in PlayerUI, not open PlayerDetailed when playing video
* gradle upped to 9.6.1, AGP upped to 9.3.0

# 12.4.0

* adopted improved API in PodciniLib 1.1.1
	* external apps (if used) need to be updated for compatibility
	* search for playlists and individual media is enabled
* added SoundCloud support via external app CloudSound
* in SearchScreen
	* changed advanced settings to a menu
	* added Remote tab for results of online media (via external apps only)
		* only performs search when the tab is open
		* media list limited by 1000, sortable
		* each media is playable (stream) and preservable
* in Episodes mode of OnlineFeed screen 
	* added sorting for episodes
	* fixed media possibly not playing when feed comes from external app
	* action button is no longer long-pressable
* in topbar of EpisodeInfo screen 
	* added a close button
	* disabled showHome button when episode is from an external app

# 12.3.1

* fixed issues of receiving shared media or feed from other apps
	* fix for YT channels should be combined with update of UT.urn app to 1.0.13

# 12.3.0

* removed hard-coded checks on youtube type
* added PeerTube support via external app PeerPop
* Feed origin filter is amended
* adopted improved API in PodciniLib 1.0.9 in handling external source
	* external app (if used) needs to be updated to 1.0.11 for compatibility
* ensure ShareReceiverActivity exits properly when done and when failed parsing shared external media

# 12.2.2

* in EpisodeInfo view, when Related is clicked, undo closing EpisodeInfo from 12.2.0
* in duplicate list, action button is not shown

# 12.2.1

* in OnlineFeed, fixed shared YT playlist not handled properly 

# 12.2.0

* in EpisodeInfo view, when Related is clicked, ensure EpisodeInfo is closed
* when an external media is shared, if it already exists, show the existing media or a list of existing duplicates
* some dependencies update

# 12.1.13

* when getting episodes from external app, break out early when number of episodes returned is smaller than requested
* PodciniLib upped to 1.0.8, external app (if used) needs to be updated to 1.0.10 for compatibility
	* feed simple updates breaks out more efficiently 
* amended toasts on external app connection
* some code refactoring

# 12.1.12

* fixed wrongly fetching chapters on YT media but not on normal podcast
* added player error handling on corrupted streams
* added extractor factory in player creation pipeline

# 12.1.11

* improved efficiency of opening EpisodeInfo from a list
* ensure to re/de-register searcher from external app when the app is connected/disconnected
* fixed in external app issue of updating YT channel live tab

# 12.1.10

* simplified scroll position handling
* fixed list scroll position not stable on return

# 12.1.9

* on topbar of FeedDetails screen, 
	* enabled History icon in Info mode
	* border is added to feed image 
* in OnlineFeed screen
	* ensure feed options dialog popup only with multiple options
	* episodes limit input is no longer instant (tap on "Setting" is needed)
* ensure viewmodels are cleared on back-press
* in Library screen, fixed strange feeds sorting behavior on feed properties

# 12.1.8

* in OnlineFeed, before subscribe, allows selecting a YT channel tab, currently default or live (streams)
* PodciniLib upped to 1.0.7, external app (if used) needs to be updated to 1.0.9 for compatibility

# 12.1.7

* in OnlineFeed
	* fixed number of episodes showing 0 for normal podcast
	* after subscribe, "Subscribe" button turns to "Open"
	* error message is shown on reopen of an erroneous feed
* in feed full update, enhanced getting episodes in external app
* getting episodes from external app is limited by 5000
* minor code refactoring

# 12.1.6

* fixed null pointer crash in FeedSettings
* in OnlineFeed, fixed episodes count is capped around 100 when feed is provided by external app,
* in feed full update
	* fixed not getting older episodes if limit is set
	* fixed failure of getting updates from external app
* in FeedDetails screen, total episodes in info bar is updated after refresh
* PodciniLib upped to 1.0.6, external app (if used) needs to be updated 1.0.8 for compatibility
* some dependencies update

# 12.1.5

* amended short description for f-doird
* removed org.gradle.workers.isolation.default from gradle.properties, likely a problem for f-droid build

# 12.1.4

* adjusted media buffer a bit to mitigate possible YT media hiccups at higher speed
* ensure to cancel previous data source preparation when new request is launched
* in PlayerDetailed screen
	* fixed the defunct force-video button on the top bar (assembling video stream may take a couple seconds)
	* amended audio-only button on the top video bar (removing video stream and resetting audio stream)
	* note both buttons reset audio/video streams, taking a pause, 
	* setting to audio-only is only necessary to save bandwidth, otherwise, video continues to play when screen is off
* added media title natural sorting algorithm, applied in FeedDetails and Queues screens
* amended logging levels in callFailed
* Kotlin upped to 2.4.0 and krdb to 3.3.4, and other dependencies updates

# 12.1.3

* in FeedDetails screen, removed the redundant feed image in the header
* update screenshots in Readme

# 12.1.2

* dice icon in Queues screen continues to next in queue
* in FeedDetails screen, top-left button opens the drawer (use system back to return)
* in FindFeeds screen
	* top-left button opens the drawer (use system back to return)
	* "Advanced" is changed to a settings icon on the top bar, "Import OPML" option is removed (do that from Settings->Import/Export)
	* "Local" is changed to "Local folder"

# 12.1.1

* in FeedDetails screen, feed image in info bar is moved to the top bar, and toggles list/info modes on click
* dice icon on info bars is padded and transparent

# 12.1.0

* on Infobar of media lists, added dice icon to randomly play an item

# 12.0.7

* removed old db migrations
* no longer logging "No New episodes found for feed"
* built with/for SDK 37

# 12.0.6

* amended feed searcher handling

# 12.0.5

* made binding to external service app more resilient
* clarified toasts when external audio streams are empty
* in OnlineFeed screen
	* enabled swipe actions but only limit to applicable actions (only SearchSelected now)
	* disabled inapplicable menu items in multi-select mode

# 12.0.4

* fixed player possibly playing previous media from external source
* corrected enum class FeedType definition
* added compatibility check when loading an external service app
* PodciniLib upped to 1.0.4

# 12.0.3

* in media player, amended ERROR_CODE_IO_UNSPECIFIED error handling
* fixed local media not playing, adopted from Podcini.X
* PodciniLib upped to 1.0.3

# 12.0.2

* capable of handling multiple external server apps
* PodciniLib upped to 1.0.2

# 12.0.1

* corrected giving direct data source at native player creation
* disabled likely unnecessary uploading duration data when playing an episode
* fixed position not set properly when playing an episode midway

# 12.0.0

* first release
