// Shared confirm/error modals, available on every page via fragments/nav :: modals.
(function () {
    // Body scroll lock: native <dialog>.showModal() doesn't stop the page behind
    // it from scrolling on its own, so without this the background remains
    // scrollable while a modal is open. Patched once here instead of wiring it
    // per dialog per page, so every <dialog> everywhere is covered automatically.
    // A counter (not a boolean) handles one modal opening on top of another
    // (e.g. the ticket modal's action-modal) without unlocking too early.
    (function () {
        var openDialogCount = 0;
        var nativeShowModal = HTMLDialogElement.prototype.showModal;

        HTMLDialogElement.prototype.showModal = function () {
            openDialogCount++;
            document.body.style.overflow = 'hidden';
            return nativeShowModal.apply(this, arguments);
        };

        // The native 'close' event fires however the dialog closed — .close(),
        // the Escape key, or a method="dialog" form submit — so this catches
        // every case without each call site reporting back separately. It
        // doesn't bubble, but capturing still reaches a document-level listener.
        document.addEventListener('close', function (e) {
            if (e.target.tagName !== 'DIALOG') {
                return;
            }
            openDialogCount = Math.max(0, openDialogCount - 1);
            if (openDialogCount === 0) {
                document.body.style.overflow = '';
            }
        }, true);
    })();

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

    // Inline "edit name in place" rows (Categories, Subcategories, Departments):
    // the "⋮" menu's "Επεξεργασία" action doesn't hold its own form — it swaps the
    // row's visible name (.entity-edit-view) for its edit form (.entity-inline-edit-form)
    // directly in the row/list item, marked with [data-entity-row].
    document.addEventListener('click', function (e) {
        var trigger = e.target.closest('.js-edit-trigger');
        if (trigger) {
            var row = trigger.closest('[data-entity-row]');
            var menu = trigger.closest('.entity-menu');
            if (menu) {
                menu.removeAttribute('open');
            }
            if (!row) {
                return;
            }
            row.querySelectorAll('.entity-edit-view').forEach(function (el) { el.hidden = true; });
            var form = row.querySelector('.entity-inline-edit-form');
            if (form) {
                form.hidden = false;
                var input = form.querySelector('input[type="text"]');
                if (input) {
                    input.focus();
                    input.select();
                }
            }
            return;
        }

        var cancel = e.target.closest('.js-edit-cancel');
        if (cancel) {
            var editRow = cancel.closest('[data-entity-row]');
            if (!editRow) {
                return;
            }
            var editForm = cancel.closest('.entity-inline-edit-form');
            if (editForm) {
                editForm.hidden = true;
                editForm.querySelectorAll('input[type="text"]').forEach(function (input) {
                    input.value = input.defaultValue;
                });
            }
            editRow.querySelectorAll('.entity-edit-view').forEach(function (el) { el.hidden = false; });
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        // Theme itself is applied synchronously by the inline head script (before
        // paint, to avoid a flash of the wrong theme); this just wires the toggle.
        var themeToggle = document.getElementById('theme-toggle');
        if (themeToggle) {
            themeToggle.addEventListener('click', function () {
                var next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
                document.documentElement.setAttribute('data-theme', next);
                localStorage.setItem('theme', next);
            });
        }

        // Chrome (and most browsers) silently ignore Notification.requestPermission()
        // unless it's called from a real user gesture — calling it on page load
        // never shows the actual prompt, it just leaves permission stuck at
        // "default" forever. So this button is the only place that ever asks.
        var notifToggle = document.getElementById('notifications-toggle');
        if (notifToggle && window.Notification) {
            notifToggle.hidden = Notification.permission !== 'default';
            notifToggle.addEventListener('click', function () {
                Notification.requestPermission().then(function (permission) {
                    notifToggle.hidden = permission !== 'default';
                    if (permission === 'denied') {
                        showError('Οι ειδοποιήσεις απορρίφθηκαν. Μπορείτε να τις ενεργοποιήσετε ξανά από τις ρυθμίσεις του browser για αυτόν τον ιστότοπο.');
                    }
                });
            });
        }

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
