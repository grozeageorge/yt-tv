function getChannelSuggestions() {
    const container = document.getElementById('ai-suggestions-container');
    const list = document.getElementById('ai-suggestions-list');

    // If AI is disabled in this environment, show a friendly message and abort.
    if (typeof aiEnabled !== 'undefined' && !aiEnabled) {
        container.style.display = 'block';
        list.innerHTML = '<span class="text-warning">AI features are disabled in this cloud environment to save resources.</span>';
        return;
    }

    // Show the container
    container.style.display = 'block';
    list.innerHTML = '<span class="text-white">Asking Nova for recommendations...</span>';

    // Call the API using the global variable 'currentPlaylistId'
    fetch('/api/ai/suggest-channels?playlistId=' + encodeURIComponent(currentPlaylistId) + '&userId=' + encodeURIComponent(currentUserId), { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            list.innerHTML = ''; // Clear loading message

            if (!data.suggestions || data.suggestions.length === 0) {
                list.innerHTML = '<span class="text-secondary">No suggestions found.</span>';
                return;
            }

            data.suggestions.forEach(name => {
                // Create a badge that acts as a quick-add form
                const badge = document.createElement('div');

                // Construct the form action URL dynamically using the ID
                const actionUrl = '/playlist/' + currentPlaylistId + '/add-channel-query';

                badge.innerHTML = `
                    <form action="${actionUrl}" method="post" style="display:inline">
                        <input type="hidden" name="query" value="${name}">
                        <input type="hidden" name="_csrf" value="${csrfToken}">
                        <button type="submit" class="btn btn-sm btn-outline-light rounded-pill m-1">
                            + ${name}
                        </button>
                    </form>
                `;
                list.appendChild(badge);
            });
        })
        .catch(err => {
            console.error(err);
            list.innerHTML = '<span class="text-danger">Error contacting AI.</span>';
        });
}