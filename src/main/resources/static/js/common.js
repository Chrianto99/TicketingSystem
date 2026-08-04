// Shared confirm/error modals, available on every page via fragments/nav :: modals.
(function () {
    function showError(message) {
        var modal = document.getElementById('error-modal');
        if (!modal) {
            window.alert(message);
            return;
        }
        document.getElementById('error-modal-message').textContent = message;
        modal.showModal();
    }

    function showConfirm(message) {
        var modal = document.getElementById('confirm-modal');
        if (!modal) {
            return Promise.resolve(window.confirm(message));
        }
        return new Promise(function (resolve) {
            document.getElementById('confirm-modal-message').textContent = message;
            var okBtn = document.getElementById('confirm-modal-ok');
            var cancelBtn = document.getElementById('confirm-modal-cancel');
            var closeBtn = document.getElementById('confirm-modal-close');

            function cleanup(result) {
                okBtn.removeEventListener('click', onOk);
                cancelBtn.removeEventListener('click', onCancel);
                closeBtn.removeEventListener('click', onCancel);
                modal.removeEventListener('cancel', onDialogCancel);
                modal.close();
                resolve(result);
            }

            function onOk() { cleanup(true); }
            function onCancel() { cleanup(false); }
            function onDialogCancel(e) { e.preventDefault(); cleanup(false); }

            okBtn.addEventListener('click', onOk);
            cancelBtn.addEventListener('click', onCancel);
            closeBtn.addEventListener('click', onCancel);
            modal.addEventListener('cancel', onDialogCancel);
            modal.showModal();
        });
    }

    window.showError = showError;
    window.showConfirm = showConfirm;

    // Close any open "⋮" options menu (.entity-menu) when clicking anywhere
    // outside of it — native <details> only closes via its own summary.
    document.addEventListener('click', function (e) {
        document.querySelectorAll('.entity-menu[open]').forEach(function (menu) {
            if (!menu.contains(e.target)) {
                menu.removeAttribute('open');
            }
        });
    });

    document.addEventListener('DOMContentLoaded', function () {
        var errorModal = document.getElementById('error-modal');
        if (errorModal) {
            var closeBtn = document.getElementById('error-modal-close');
            var okBtn = document.getElementById('error-modal-ok');
            closeBtn.addEventListener('click', function () { errorModal.close(); });
            okBtn.addEventListener('click', function () { errorModal.close(); });
        }

        var serverError = document.getElementById('server-error-message');
        if (serverError && serverError.textContent.trim()) {
            showError(serverError.textContent.trim());
        }
    });

    // HTML5 "required" lets whitespace-only text through; @NotBlank on the server doesn't.
    // Catch that here so the user gets instant feedback instead of a round-trip.
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }
        var commentFields = form.querySelectorAll('[name="commentText"], [name="text"]');
        for (var i = 0; i < commentFields.length; i++) {
            var field = commentFields[i];
            if (field.hasAttribute('required') && !field.value.trim()) {
                e.preventDefault();
                e.stopImmediatePropagation();
                field.focus();
                showError('Το σχόλιο δεν μπορεί να είναι κενό.');
                return;
            }
        }
    }, true);

    // Global "press N for New Ticket" shortcut, available on every page. On /tickets
    // it opens the create-ticket modal directly (see tickets.js); anywhere else it
    // navigates there with ?openCreate=true, which tickets.js picks up on load.
    document.addEventListener('keydown', function (e) {
        if (e.key !== 'n' && e.key !== 'N') {
            return;
        }
        if (e.ctrlKey || e.metaKey || e.altKey) {
            return;
        }
        var target = e.target;
        var tag = target && target.tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || (target && target.isContentEditable)) {
            return;
        }
        if (document.querySelector('dialog[open]')) {
            return;
        }
        e.preventDefault();
        if (typeof window.openCreateTicketModal === 'function') {
            window.openCreateTicketModal();
        } else {
            window.location.href = '/tickets?openCreate=true';
        }
    });

    // Replaces onsubmit="return confirm(...)" across the app: add data-confirm="message"
    // to any <form> and it will be intercepted and re-submitted only after confirmation.
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (!(form instanceof HTMLFormElement) || !form.hasAttribute('data-confirm') || form.dataset.confirmed) {
            return;
        }
        e.preventDefault();
        showConfirm(form.getAttribute('data-confirm')).then(function (ok) {
            if (!ok) {
                return;
            }
            form.dataset.confirmed = 'true';
            if (form.requestSubmit) {
                form.requestSubmit();
            } else {
                form.submit();
            }
            delete form.dataset.confirmed;
        });
    }, true);
})();
