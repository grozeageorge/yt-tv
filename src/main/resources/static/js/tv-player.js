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
    console.log("Error playing video index " + currentIndex + ". Skipping...");
    nextVideo();
}

function nextVideo() {
    markAsWatched(videoQueue[currentIndex]);

    currentIndex++;
    if (currentIndex >= videoQueue.length) {
        console.log("Queue finished. Reloading to fetch next batch...");
        document.getElementById('now-playing').innerText = "Fetching new videos from TV...";
        window.location.reload();
        return;
    }
    // Load AND Play the next video
    // (This usually works perfectly automatically after the first click)
    player.loadVideoById(videoQueue[currentIndex]);
    updateTitle();
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