let currentVideoQueue = [];
let lastUserQuery = "";

function handleEnter(e) {
    if (e.key === 'Enter') sendMessage();
}

function sendMessage() {
    const input = document.getElementById('user-input');
    const text = input.value.trim();
    if (!text) return;

    lastUserQuery = text;

    // 1. Show User Message
    addMessage(text, 'user-msg');
    input.value = '';

    // 2. Send to Backend
    const loadingId = 'loading-' + Date.now();
    addMessage("Thinking...", 'ai-msg', loadingId);

    fetch('/api/ai/chat-full?message=' + encodeURIComponent(text), { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            const loader = document.getElementById(loadingId);
            if (loader) loader.remove();

            addMessage(data.message, 'ai-msg');

            // Store videos and Show Button
            if (data.videoIds && data.videoIds.length > 0) {
                currentVideoQueue = data.videoIds;

                const box = document.getElementById('chat-box');
                const btnDiv = document.createElement('div');
                btnDiv.className = "text-center mb-3";
                // Only this button calls startPlayer() now
                btnDiv.innerHTML = `<button type="button" class="btn btn-sm btn-outline-success" onclick="startPlayer()">&#9658; Play Found Videos (${data.videoIds.length})</button>`;
                box.appendChild(btnDiv);
                box.scrollTop = box.scrollHeight;
            }
        })
        .catch(err => {
            console.error(err);
            const loader = document.getElementById(loadingId);
            if (loader) loader.innerText = "Error contacting AI.";
        });
}

function addMessage(text, className, id) {
    const div = document.createElement('div');
    div.className = `message ${className}`;
    div.innerText = text;
    if (id) div.id = id;

    const box = document.getElementById('chat-box');
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

function startPlayer() {
    console.log("Attempting to start player...");

    if (!currentVideoQueue || currentVideoQueue.length === 0) {
        alert("Queue expired or empty. Ask for new videos.");
        return;
    }

    const inputField = document.getElementById('videoIdsInput');
    if(inputField) {
        inputField.value = currentVideoQueue.join(',');

        // Inject Query for Auto-DJ
        const form = document.getElementById('playForm');
        let queryInput = document.getElementById('queryInput');
        if (!queryInput) {
            queryInput = document.createElement("input");
            queryInput.type = "hidden";
            queryInput.name = "originalQuery";
            queryInput.id = "queryInput";
            form.appendChild(queryInput);
        }
        queryInput.value = lastUserQuery;

        form.submit();
    }
}