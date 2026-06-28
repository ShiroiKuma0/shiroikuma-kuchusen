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
* PodciniLib upped to 1.0.7, external app (if used) needs to be updated for compatibility

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
* PodciniLib upped to 1.0.6, external app (if used) needs to be updated for compatibility
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
