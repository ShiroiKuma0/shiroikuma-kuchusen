# 12.1.1

* amended FeedDetails top/info bars
	* feed image is moved to the top bar, and toggles list/info modes
	* dice icon is padded and transparent

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
