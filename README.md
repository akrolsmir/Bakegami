Background Refresh
------------------

Tired of stock wallpapers? Background Refresh pulls popular images onto your device and automatically sets them as your background! 

* Choose any category of images you'd like to see!
* Configure it to update on a weekly, daily, or even hourly basis!
* Download and save your favorites with one click!
* Power user? Access shortcuts from the handy widget!

Free and open source, Background Refresh aims to improve your Android experience by delivering quality content straight to your background.

Fix:
* Resize gigantic images - http://stackoverflow.com/questions/10271020/bitmap-too-large-to-be-uploaded-into-a-texture
* GridView - Chooser Always
* Gridview - Set selected as BG
* [Widget] Favorite button colors when clicked
* [Widget] Unfavorite on next click
* [Setting] Toggle cycle on boot

Ideally:
* Reset time counter on skip
* Figure out defaults
* [Refactor] putExtra instead of setAction
* Allow user to adjust wallpaper
* Retry failed wallpapers

* Use progressbar to show time until next wallpaper
* Toggle download on wifi only
* Grab images from RES
* Undo delete - https://github.com/soarcn/UndoBar/blob/master/example/src/com/cocosw/undobar/example/UndoStyle.java
* Dynamic grid sizing ala Scrolldit
* Improve landscape layout
* Animate transitions - http://stackoverflow.com/a/6857762/1222351
* Grab images from next page on exhaust / Cycle through favorites