document.addEventListener('DOMContentLoaded', function () {
  const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
  const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
  const csrfToken = csrfTokenMeta ? csrfTokenMeta.getAttribute('content') : '';
  const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : 'X-CSRF-TOKEN';

  function interceptSyncForm(form) {
    form.addEventListener('submit', function (e) {
      try {
        e.preventDefault();
        const action = form.getAttribute('action') || '';
        if (!action.endsWith('/channels/sync')) {
          form.submit();
          return;
        }

        const formData = new FormData(form);
        const params = new URLSearchParams();
        for (const [k, v] of formData.entries()) {
          params.append(k, v);
        }

        fetch(action, {
          method: 'POST',
          headers: Object.assign({ 'Content-Type': 'application/x-www-form-urlencoded' }, csrfToken ? { [csrfHeader]: csrfToken } : {}),
          body: params.toString()
        })
          .then(res => {
            if (res.ok) {
              alert('Channel sync started successfully. New videos will appear shortly.');
              // If playlistId is present, redirect back to its page; else reload
              const playlistId = formData.get('playlistId');
              if (playlistId) {
                window.location.href = `/playlist/${playlistId}`;
              } else {
                window.location.reload();
              }
            } else {
              return res.text().then(t => { throw new Error(t || ('HTTP ' + res.status)); });
            }
          })
          .catch(err => {
            console.error('Sync failed', err);
            alert('Failed to start sync: ' + (err && err.message ? err.message : 'Unknown error'));
          });
      } catch (ex) {
        console.error('Sync handler error', ex);
        // fallback to normal submit
        form.submit();
      }
    });
  }

  // Attach to all forms that post to /channels/sync
  document.querySelectorAll('form[action$="/channels/sync"][method="post"]').forEach(interceptSyncForm);
});
