let currentIndex = 0;
let player;

// 1. Load the YouTube IFrame API
var tag = document.createElement('script');
tag.src = "https://www.youtube.com/iframe_api";
var firstScriptTag = document.getElementsByTagName('script')[0];
firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

function onYouTubeIframeAPIReady() {
    if (typeof videoQueue === 'undefined' || videoQueue.length === 0) {
        document.getElementById('now-playing').innerText = "No videos to play.";
        return;
    }

    player = new YT.Player('player', {
        height: '100%',
        width: '100%',
        // We do NOT set 'videoId' here. We load it manually in onReady.
        playerVars: {
            'autoplay': 1,
            'controls': 1,
            'rel': 0,
            'fs': 1,
            'enablejsapi': 1,
            'origin': window.location.origin
        },
        events: {
            'onReady': onPlayerReady,
            'onStateChange': onPlayerStateChange,
            'onError': onPlayerError
        }
    });
}

function onPlayerReady(event) {
    // Simple approach: Just load and try to play
    // If the browser blocks audio, it will wait for the user to click play.
    player.loadVideoById(videoQueue[currentIndex]);
    updateTitle();
}

function onPlayerStateChange(event) {
    // When video ends (0), play next
    if (event.data === 0) {
        nextVideo();
    }
}

function onPlayerError(event) {
    let reason = "Unknown error."
    if (event.data === 100) reason = "Video removed.";
    if (event.data === 101 || event.data == 150) reason = "Video is restricted.";

    console.log(`Skipping video index ${currentIndex} because of error: ${reason}`);

    const status = document.getElementById('now-playing');
    if (status) status.innerText = `Skipping: ${reason}...`;

    setTimeout(() => nextVideo(), 1000);
}

function nextVideo() {
    markAsWatched(videoQueue[currentIndex]);

    currentIndex++;
    if (currentIndex >= videoQueue.length) {

        if (currentPlaylistId !== null) {
            console.log("Playlist ended. Fetching next batch...");
            document.getElementById('now-playing').innerText = "Fetching new videos...";
            window.location.reload();
        }
        else if (originalAiQuery !== null) {
            console.log("AI Queue ended. Auto-fetching more for: " + originalAiQuery);
            document.getElementById('now-playing').innerText = "AI is finding more videos...";
            fetchNextAiBatch();
        }
        else {
            document.getElementById('now-playing').innerText="End of Queue.";
        }

        return;
    }
    // Load AND Play the next video
    // (This usually works perfectly automatically after the first click)
    player.loadVideoById(videoQueue[currentIndex]);
    updateTitle();
}

function fetchNextAiBatch() {
    fetch('/api/ai/chat-full?message=' + encodeURIComponent(originalAiQuery), { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            if (data.videoIds && data.videoIds.length > 0) {
                // We found more! Reload the player with the new list.
                document.getElementById('nextBatchIds').value = data.videoIds.join(',');
                document.getElementById('nextBatchQuery').value = originalAiQuery;
                document.getElementById('aiReloadForm').submit();
            } else {
                document.getElementById('now-playing').innerText = "No more videos found!";
            }
        })
        .catch(err => console.error(err));
}

function updateTitle() {
    const infoText = "Playing Video " + (currentIndex + 1) + " of " + videoQueue.length;
    document.getElementById('now-playing').innerText = infoText;
}

function onPlayerStateChange(event) {
    // When video ends (0)
    if (event.data === 0) {
        markAsWatched(videoQueue[currentIndex]); // Call backend
        nextVideo();
    }
}

// NEW FUNCTION: Call the Java Backend
function markAsWatched(videoId) {
    console.log("Marking watched: " + videoId);
    fetch('/api/watched/' + videoId, {
        method: 'POST'
    }).then(response => {
        if (!response.ok) console.error("Failed to mark watched");
    });
}