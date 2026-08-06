document.addEventListener('DOMContentLoaded', function () {
    var STATUS_LABELS = { OPEN: 'Ανοιχτό', RESOLVED: 'Επιλύθηκε', CANCELLED: 'Ακυρώθηκε' };
    var PRIORITY_LABELS = { LOW: 'Χαμηλή', MEDIUM: 'Μεσαία', HIGH: 'Υψηλή', CRITICAL: 'Κρίσιμη' };

    var modal = document.getElementById('ticket-modal');
    var modalTitle = document.getElementById('modal-title');
    var modalStatusBadge = document.getElementById('modal-status-badge');
    var modalPriorityBadge = document.getElementById('modal-priority-badge');
    var modalMeta = document.getElementById('modal-meta');
    var modalBody = document.getElementById('modal-body');
    var modalFooter = document.getElementById('modal-footer');
    var currentUserId = document.body.getAttribute('data-current-user-id');
    var actionModal = document.getElementById('action-modal');
    var actionModalForm = document.getElementById('action-modal-form');

    if (!modal) {
        return;
    }

    // Live search-by-description: auto-submits the filters form (carrying over
    // every other active filter) shortly after the user stops typing, instead of
    // requiring an explicit search button.
    var descriptionSearchInput = document.getElementById('ticket-description-search');
    if (descriptionSearchInput) {
        var descriptionSearchTimer = null;
        descriptionSearchInput.addEventListener('input', function () {
            clearTimeout(descriptionSearchTimer);
            descriptionSearchTimer = setTimeout(function () {
                descriptionSearchInput.form.submit();
            }, 400);
        });
        if (descriptionSearchInput.value) {
            descriptionSearchInput.focus();
            var caretPos = descriptionSearchInput.value.length;
            descriptionSearchInput.setSelectionRange(caretPos, caretPos);
        }
    }

    if (actionModal) {
        document.getElementById('action-modal-close').addEventListener('click', function () {
            actionModal.close();
        });
        document.getElementById('action-modal-cancel').addEventListener('click', function () {
            actionModal.close();
        });
        actionModalForm.addEventListener('submit', function () {
            document.getElementById('action-modal-submit').disabled = true;
        });
    }

    document.querySelectorAll('.ticket-row').forEach(function (row) {
        row.addEventListener('click', function () {
            openTicketModal(row.getAttribute('data-ticket-id'));
        });
    });

    var openTicketParam = new URLSearchParams(window.location.search).get('openTicket');
    if (openTicketParam) {
        openTicketModal(openTicketParam);
        var cleanUrl = window.location.pathname + window.location.hash;
        window.history.replaceState(null, '', cleanUrl);
    }

    function openTicketModal(ticketId, activeTab) {
        modalTitle.textContent = 'Ticket #' + ticketId;
        modalStatusBadge.style.display = 'none';
        modalPriorityBadge.style.display = 'none';
        modalMeta.textContent = '';
        modalBody.innerHTML = '<p>Φόρτωση…</p>';
        modalFooter.innerHTML = '';
        modalFooter.style.display = 'none';
        var headerMenu = document.getElementById('ticket-modal-menu');
        headerMenu.hidden = true;
        headerMenu.removeAttribute('open');
        modal.showModal();

        Promise.all([
            fetch('/api/tickets/' + ticketId).then(handleResponse),
            fetch('/api/tickets/' + ticketId + '/history').then(handleResponse),
            fetch('/api/tickets/' + ticketId + '/attachments').then(handleResponse)
        ]).then(function (results) {
            renderTicket(results[0], results[1], results[2]);
            if (activeTab) {
                activateTab(activeTab);
            }
        }).catch(function (err) {
            modal.close();
            window.showError('Αποτυχία φόρτωσης ticket: ' + err.message);
        });
    }

    function handleResponse(response) {
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        return response.json();
    }

    function renderTicket(ticket, history, attachments) {
        modalTitle.textContent = 'Ticket #' + ticket.id;
        modalStatusBadge.textContent = STATUS_LABELS[ticket.status] || ticket.status;
        modalStatusBadge.className = 'badge status-' + ticket.status;
        modalStatusBadge.style.display = '';
        modalPriorityBadge.textContent = PRIORITY_LABELS[ticket.priority] || ticket.priority;
        modalPriorityBadge.className = 'badge priority-' + ticket.priority;
        modalPriorityBadge.style.display = '';
        modalMeta.textContent = 'Δημιουργήθηκε από ' + (ticket.creatorUsername || 'διαγραμμένο χρήστη') + ' ' + formatRelativeTime(ticket.createdAt);

        var html = '';

        html += '<div class="modal-tabs" role="tablist">';
        html += '<button type="button" class="modal-tab active" data-tab-target="td-tab-info" role="tab" aria-selected="true">Πληροφορίες</button>';
        html += '<button type="button" class="modal-tab" data-tab-target="td-tab-history" role="tab" aria-selected="false">Ιστορικό</button>';
        html += '<button type="button" class="modal-tab" data-tab-target="td-tab-comments" role="tab" aria-selected="false">Σχόλια</button>';
        html += '</div>';

        html += '<div id="td-tab-info" class="modal-tab-panel">';
        html += '<h2 class="ticket-summary-title">' + escapeHtml(ticket.summary || '—') + '</h2>';
        html += '<div id="info-fields">' + infoFieldsHtml(ticket, attachments) + '</div>';
        html += '</div>';

        var actionHistory = history.filter(function (h) { return h.action !== 'COMMENT_ADDED'; });
        html += '<div id="td-tab-history" class="modal-tab-panel" style="display:none;">';
        if (!actionHistory.length) {
            html += '<p class="field-label" style="text-align:center;">Δεν υπάρχει ιστορικό ακόμα.</p>';
        } else {
            html += '<div class="timeline">';
            actionHistory.forEach(function (h) {
                html += historyItemHtml(h);
            });
            html += '</div>';
        }
        html += '</div>';

        var comments = history.filter(function (h) { return h.commentText; }).reverse();
        html += '<div id="td-tab-comments" class="modal-tab-panel" style="display:none;">';
        if (ticket.status === 'OPEN') {
            html += composeCommentHtml(ticket.id);
        }
        if (!comments.length) {
            html += '<p class="field-label" style="text-align:center;">Δεν υπάρχουν σχόλια ακόμα.</p>';
        } else {
            html += '<div class="timeline">';
            comments.forEach(function (h) {
                html += commentTabItemHtml(h, ticket.id);
            });
            html += '</div>';
        }
        html += '</div>';

        modalBody.innerHTML = html;
        wireReassign();
        wireResolveForm(ticket);
        wireCommentTabEdits();
        wireCommentCompose(ticket.id);
        wireModalTabs();
        wireAttachments(ticket.id);
        wireHeaderMenu(ticket, attachments);

        modalFooter.innerHTML = actionAreaHtml(ticket);
        modalFooter.style.display = '';
        wireActionButtons(ticket);
    }

    // "⋮" header menu: Επεξεργασία (edit info, creator-only) and Ακύρωση Ticket
    // (only while OPEN — matches the same rules the backend already enforces).
    function wireHeaderMenu(ticket, attachments) {
        var menu = document.getElementById('ticket-modal-menu');
        var editBtn = document.getElementById('ticket-menu-edit-btn');
        var cancelBtn = document.getElementById('ticket-menu-cancel-btn');
        var divider = menu.querySelector('.entity-menu-divider');

        var isAdmin = document.body.getAttribute('data-is-admin') === 'true';
        var canEdit = currentUserId && (isAdmin || String(ticket.creatorId) === currentUserId);
        var canCancel = ticket.status === 'OPEN';

        editBtn.hidden = !canEdit;
        cancelBtn.hidden = !canCancel;
        divider.hidden = !(canEdit && canCancel);
        menu.hidden = !canEdit && !canCancel;
        menu.removeAttribute('open');

        editBtn.onclick = canEdit ? function () {
            menu.removeAttribute('open');
            activateTab('td-tab-info');
            document.getElementById('info-fields').innerHTML = infoEditFormHtml(ticket);
            wireInfoEdit(ticket, attachments);
        } : null;

        cancelBtn.onclick = canCancel ? function () {
            menu.removeAttribute('open');
            openActionModal({
                action: '/tickets/' + ticket.id + '/cancel',
                title: 'Ακύρωση Ticket',
                label: 'Λόγος ακύρωσης',
                placeholder: 'Περιγράψτε τον λόγο ακύρωσης…',
                submitLabel: 'Ακύρωση Ticket',
                needsSubcategory: false
            });
        } : null;
    }

    function infoFieldsHtml(ticket, attachments) {
        var html = '';

        html += fieldBlock('Όνομα καλούντος', escapeHtml(ticket.callerName || '—'));
        html += fieldBlock('Αριθμός τηλεφώνου', escapeHtml(ticket.phoneNumber || '—'));
        html += fieldBlock('Τμήμα', escapeHtml(ticket.departmentName || '—'));
        if (ticket.ipAddress) {
            html += fieldBlock('Διεύθυνση IP', escapeHtml(ticket.ipAddress));
        }

        var categoryValue = ticket.category + (ticket.subcategory ? ' → ' + ticket.subcategory : '');
        html += fieldBlock('Κατηγορία Βλάβης', escapeHtml(categoryValue));

        var assigneeValue = '<div class="assignee-view" id="assignee-view">';
        assigneeValue += '<span id="td-assignee">' + escapeHtml(ticket.assignedUsername || 'Χωρίς ανάθεση') + '</span>';
        if (ticket.status === 'OPEN') {
            assigneeValue += '<button type="button" class="btn btn-sm" id="td-reassign-toggle">Ανάθεση σε</button>';
        }
        assigneeValue += '</div>';
        html += fieldBlock('Ανατέθηκε σε', assigneeValue);

        if (ticket.status === 'OPEN') {
            var csrfInputForReassign = document.querySelector('#create-ticket-modal input[name="_csrf"]');
            var reassignCsrfToken = csrfInputForReassign ? csrfInputForReassign.value : '';
            html += '<form id="td-reassign-form" class="assignee-edit" method="post" action="/tickets/' + ticket.id + '/reassign" style="display:none;">';
            html += '<input type="hidden" name="_csrf" value="' + escapeHtml(reassignCsrfToken) + '">';
            html += '<select name="assignedTo" id="td-reassign-select"></select>';
            html += '<input type="text" name="commentText" placeholder="Λόγος (προαιρετικό)">';
            html += '<button type="submit" class="btn btn-primary btn-sm">Αποθήκευση</button>';
            html += '<button type="button" class="btn btn-sm" id="td-reassign-cancel">Άκυρο</button>';
            html += '</form>';
        }

        html += fieldBlock('Λεπτομέρειες', escapeHtml(ticket.description || '—'));

        html += '<div class="field-label" style="margin-top:10px;">Λύση</div>';
        html += '<div class="description-block" id="resolution-view">' + escapeHtml(ticket.resolution || '—') + '</div>';
        if (ticket.status === 'OPEN') {
            html += '<form id="td-resolve-form" class="resolve-edit" style="display:none;">';
            html += '<textarea id="td-resolve-textarea" placeholder="Περιγράψτε τη λύση…">' + escapeHtml(ticket.resolution || '') + '</textarea>';
            if (!ticket.subcategoryId) {
                html += '<select id="td-resolve-subcategory"><option value="">Φόρτωση…</option></select>';
            }
            html += '<div class="ticket-action-buttons">';
            html += '<button type="button" class="btn btn-icon-only" id="td-resolve-cancel" title="Άκυρο" aria-label="Άκυρο">✗</button>';
            html += '<button type="submit" class="btn btn-primary btn-icon-only" id="td-resolve-submit" title="Επιβεβαίωση" aria-label="Επιβεβαίωση">✓</button>';
            html += '</div>';
            html += '</form>';
        }

        html += '<div class="field-label attachments-heading" style="margin-top:10px;">';
        html += '<span>Επισυναπτόμενα</span>';
        html += '<button type="button" class="icon-btn" id="td-attachment-toggle" title="Επισύναψη αρχείου">📎</button>';
        html += '</div>';
        html += '<input type="file" id="td-attachment-input" hidden>';
        html += '<div id="td-attachment-list">' + attachmentsHtml(attachments) + '</div>';

        return html;
    }

    function fieldBlock(label, valueHtml) {
        return '<div class="field-label" style="margin-top:10px;">' + escapeHtml(label) + '</div>' +
            '<div class="description-block">' + valueHtml + '</div>';
    }

    function infoEditFormHtml(ticket) {
        var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
        var csrfToken = csrfInput ? csrfInput.value : '';

        var html = '<form id="td-edit-form" class="ticket-form" method="post" action="/tickets/' + ticket.id + '/edit">';
        html += '<input type="hidden" name="_csrf" value="' + escapeHtml(csrfToken) + '">';

        html += '<div class="form-group">';
        html += '<label for="td-edit-summary">Τίτλος</label>';
        html += '<textarea id="td-edit-summary" name="summary">' + escapeHtml(ticket.summary || '') + '</textarea>';
        html += '</div>';

        html += '<div class="form-group">';
        html += '<label for="td-edit-caller-name">Όνομα καλούντος</label>';
        html += '<input type="text" id="td-edit-caller-name" name="callerName" value="' + escapeHtml(ticket.callerName || '') + '">';
        html += '</div>';

        html += '<div class="form-group">';
        html += '<label for="td-edit-phone">Αριθμός τηλεφώνου<span class="required-mark">*</span></label>';
        html += '<input type="text" id="td-edit-phone" name="phoneNumber" value="' + escapeHtml(ticket.phoneNumber || '') + '" required="required">';
        html += '</div>';

        html += '<div class="form-group">';
        html += '<label for="td-edit-department">Τμήμα</label>';
        html += '<select id="td-edit-department" name="departmentId" required="required"></select>';
        html += '</div>';

        html += '<div class="form-group">';
        html += '<label for="td-edit-ip">Διεύθυνση IP</label>';
        html += '<input type="text" id="td-edit-ip" name="ipAddress" value="' + escapeHtml(ticket.ipAddress || '') + '" placeholder="Προαιρετικό" ' +
            'pattern="^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$" title="Πρέπει να είναι έγκυρη διεύθυνση IPv4">';
        html += '</div>';

        html += '<div class="form-group">';
        html += '<label for="td-edit-category">Κατηγορία Βλάβης<span class="required-mark">*</span></label>';
        html += '<select id="td-edit-category" name="categoryId" required="required"></select>';
        html += '</div>';

        html += '<div class="form-group">';
        html += '<label for="td-edit-subcategory">Υποκατηγορία</label>';
        html += '<select id="td-edit-subcategory" name="subcategoryId"><option value="">Επιλέξτε πρώτα κατηγορία βλάβης</option></select>';
        html += '</div>';

        html += '<div class="form-group">';
        html += '<label for="td-edit-priority">Προτεραιότητα<span class="required-mark">*</span></label>';
        html += '<select id="td-edit-priority" name="priority" required="required"></select>';
        html += '</div>';

        html += '<div class="form-group">';
        html += '<label for="td-edit-description">Λεπτομέρειες<span class="required-mark">*</span></label>';
        html += '<textarea id="td-edit-description" name="description" required="required">' + escapeHtml(ticket.description || '') + '</textarea>';
        html += '</div>';

        html += '<div class="ticket-action-buttons">';
        html += '<button type="button" class="btn" id="td-edit-cancel-btn">Άκυρο</button>';
        html += '<button type="submit" class="btn btn-primary">Αποθήκευση</button>';
        html += '</div>';
        html += '</form>';
        return html;
    }

    function wireInfoEdit(ticket, attachments) {
        var deptSelect = document.getElementById('td-edit-department');
        var catSelect = document.getElementById('td-edit-category');
        var subcatSelect = document.getElementById('td-edit-subcategory');
        var prioritySelect = document.getElementById('td-edit-priority');
        var cancelBtn = document.getElementById('td-edit-cancel-btn');

        cloneOptionsExcludingBlank('#ct-department', deptSelect);
        ensureCurrentOptionSelected(deptSelect, ticket.departmentId, ticket.departmentName);

        cloneOptionsExcludingBlank('#ct-category', catSelect);
        ensureCurrentOptionSelected(catSelect, ticket.categoryId, ticket.category);

        cloneOptionsExcludingBlank('#ct-priority', prioritySelect);
        prioritySelect.value = ticket.priority;

        loadEditSubcategories(ticket.categoryId, ticket.subcategoryId);

        catSelect.addEventListener('change', function () {
            loadEditSubcategories(catSelect.value, null);
        });

        cancelBtn.addEventListener('click', function () {
            document.getElementById('info-fields').innerHTML = infoFieldsHtml(ticket, attachments);
            wireReassign();
            wireAttachments(ticket.id);
        });
    }

    function cloneOptionsExcludingBlank(sourceSelector, targetSelect) {
        targetSelect.innerHTML = '';
        document.querySelectorAll(sourceSelector + ' option').forEach(function (opt) {
            if (!opt.value) {
                return;
            }
            var clone = opt.cloneNode(true);
            clone.selected = false;
            targetSelect.appendChild(clone);
        });
    }

    function ensureCurrentOptionSelected(selectEl, id, label) {
        if (!id) {
            return;
        }
        selectEl.value = id;
        if (String(selectEl.value) !== String(id)) {
            var opt = document.createElement('option');
            opt.value = id;
            opt.textContent = (label || 'Άγνωστο') + ' (Ανενεργό)';
            selectEl.appendChild(opt);
            selectEl.value = id;
        }
    }

    function loadEditSubcategories(categoryId, selectedId) {
        var subcatSelect = document.getElementById('td-edit-subcategory');
        if (!categoryId) {
            subcatSelect.innerHTML = '<option value="">Επιλέξτε πρώτα κατηγορία βλάβης</option>';
            subcatSelect.disabled = true;
            return;
        }
        subcatSelect.disabled = true;
        subcatSelect.innerHTML = '<option value="">Φόρτωση…</option>';

        fetch('/api/subcategories?categoryId=' + encodeURIComponent(categoryId))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (subcategories) {
                var html = '<option value="">Προαιρετικό</option>';
                subcategories.forEach(function (sc) {
                    if (!sc.active && String(sc.id) !== String(selectedId)) {
                        return;
                    }
                    html += '<option value="' + sc.id + '">' + escapeHtml(sc.name) + (sc.active ? '' : ' (Ανενεργή)') + '</option>';
                });
                subcatSelect.innerHTML = html;
                if (selectedId) {
                    subcatSelect.value = selectedId;
                }
                subcatSelect.disabled = false;
            })
            .catch(function () {
                subcatSelect.innerHTML = '<option value="">Αποτυχία φόρτωσης υποκατηγοριών</option>';
            });
    }

    function wireReassign() {
        var toggle = document.getElementById('td-reassign-toggle');
        if (!toggle) {
            return;
        }
        var view = document.getElementById('assignee-view');
        var form = document.getElementById('td-reassign-form');
        var select = document.getElementById('td-reassign-select');
        var cancelBtn = document.getElementById('td-reassign-cancel');

        var sourceOptions = document.querySelectorAll('#ct-assignee option');
        select.innerHTML = '';
        sourceOptions.forEach(function (opt) {
            var clone = opt.cloneNode(true);
            clone.selected = false;
            select.appendChild(clone);
        });

        toggle.addEventListener('click', function () {
            view.style.display = 'none';
            form.style.display = 'flex';
        });

        cancelBtn.addEventListener('click', function () {
            form.style.display = 'none';
            view.style.display = '';
        });
    }

    // The "Επίλυση Ticket" footer button doesn't open a modal — it reveals this
    // form in place of the read-only Λύση value (wired here) and focuses it.
    function wireResolveForm(ticket) {
        var form = document.getElementById('td-resolve-form');
        if (!form) {
            return;
        }
        var view = document.getElementById('resolution-view');
        var textarea = document.getElementById('td-resolve-textarea');
        var subcatSelect = document.getElementById('td-resolve-subcategory');
        var cancelBtn = document.getElementById('td-resolve-cancel');

        if (subcatSelect) {
            loadResolveSubcategories(ticket.categoryId);
        }

        cancelBtn.addEventListener('click', function () {
            form.style.display = 'none';
            view.style.display = '';
        });

        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var resolutionText = textarea.value.trim();
            if (!resolutionText) {
                window.showError('Πρέπει να περιγράψετε τη λύση.');
                return;
            }
            if (subcatSelect && !subcatSelect.value) {
                window.showError('Απαιτείται υποκατηγορία για την επίλυση ενός ticket.');
                return;
            }

            var submitBtn = document.getElementById('td-resolve-submit');
            submitBtn.disabled = true;

            var payload = { commentText: resolutionText };
            if (subcatSelect && subcatSelect.value) {
                payload.subcategoryId = subcatSelect.value;
            }

            var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
            var headers = { 'Content-Type': 'application/json' };
            if (csrfInput) {
                headers['X-CSRF-TOKEN'] = csrfInput.value;
            }

            fetch('/api/tickets/' + ticket.id + '/resolve', {
                method: 'PATCH',
                headers: headers,
                body: JSON.stringify(payload)
            }).then(function (response) {
                if (!response.ok) {
                    return response.json().catch(function () {
                        return {};
                    }).then(function (body) {
                        throw new Error(body.error || Object.values(body)[0] || ('HTTP ' + response.status));
                    });
                }
                return response.json();
            }).then(function () {
                openTicketModal(ticket.id, 'td-tab-info');
            }).catch(function (err) {
                submitBtn.disabled = false;
                window.showError('Αποτυχία επίλυσης ticket: ' + err.message);
            });
        });
    }

    function loadResolveSubcategories(categoryId) {
        var subcatSelect = document.getElementById('td-resolve-subcategory');
        subcatSelect.disabled = true;
        subcatSelect.innerHTML = '<option value="">Φόρτωση…</option>';

        fetch('/api/subcategories?categoryId=' + encodeURIComponent(categoryId))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (subcategories) {
                var active = subcategories.filter(function (sc) { return sc.active; });
                var html;
                if (active.length) {
                    html = '<option value="">Επιλέξτε υποκατηγορία…</option>';
                    active.forEach(function (sc) {
                        html += '<option value="' + sc.id + '">' + escapeHtml(sc.name) + '</option>';
                    });
                } else {
                    html = '<option value="">Δεν υπάρχουν διαθέσιμες υποκατηγορίες</option>';
                }
                subcatSelect.innerHTML = html;
                subcatSelect.disabled = false;
            })
            .catch(function () {
                subcatSelect.innerHTML = '<option value="">Αποτυχία φόρτωσης υποκατηγοριών</option>';
            });
    }

    function wireModalTabs() {
        modalBody.querySelectorAll('.modal-tab').forEach(function (tab) {
            tab.addEventListener('click', function () {
                activateTab(tab.getAttribute('data-tab-target'));
            });
        });
    }

    function activateTab(tabTarget) {
        modalBody.querySelectorAll('.modal-tab').forEach(function (t) {
            var isTarget = t.getAttribute('data-tab-target') === tabTarget;
            t.classList.toggle('active', isTarget);
            t.setAttribute('aria-selected', isTarget ? 'true' : 'false');
        });
        modalBody.querySelectorAll('.modal-tab-panel').forEach(function (panel) {
            panel.style.display = panel.id === tabTarget ? '' : 'none';
        });
    }

    function actionAreaHtml(ticket) {
        var isOpen = ticket.status === 'OPEN';
        var html = '<div class="ticket-action-area">';

        if (isOpen) {
            html += '<div class="ticket-action-buttons ticket-action-buttons-center">';
            html += '<button type="button" class="btn btn-primary" id="ticket-resolve-btn">Επίλυση Ticket</button>';
            html += '</div>';
        } else {
            var isCancelled = ticket.status === 'CANCELLED';
            var isAdmin = document.body.getAttribute('data-is-admin') === 'true';

            html += '<div class="ticket-action-buttons ticket-action-buttons-center">';
            html += '<button type="button" class="btn btn-primary" id="ticket-reopen-btn">↺ Επανάνοιγμα</button>';
            if (isCancelled && isAdmin) {
                html += '<button type="button" class="btn btn-danger" id="ticket-delete-btn">🗑 Διαγραφή</button>';
            }
            html += '</div>';
        }

        html += '</div>';
        return html;
    }

    function wireActionButtons(ticket) {
        var isOpen = ticket.status === 'OPEN';

        if (isOpen) {
            document.getElementById('ticket-resolve-btn').addEventListener('click', function () {
                activateTab('td-tab-info');
                document.getElementById('resolution-view').style.display = 'none';
                document.getElementById('td-resolve-form').style.display = 'flex';
                document.getElementById('td-resolve-textarea').focus();
            });
        } else {
            document.getElementById('ticket-reopen-btn').addEventListener('click', function () {
                openActionModal({
                    action: '/tickets/' + ticket.id + '/reopen',
                    title: 'Επανάνοιγμα Ticket',
                    label: 'Λόγος επανανοίγματος',
                    placeholder: 'Περιγράψτε γιατί το επανανοίγετε…',
                    submitLabel: 'Επανάνοιγμα',
                    needsSubcategory: false
                });
            });

            var deleteBtn = document.getElementById('ticket-delete-btn');
            if (deleteBtn) {
                deleteBtn.addEventListener('click', function () {
                    window.showConfirm('Διαγραφή αυτού του ακυρωμένου ticket; Αυτή η ενέργεια δεν μπορεί να αναιρεθεί.').then(function (ok) {
                        if (!ok) {
                            return;
                        }
                        var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
                        var headers = {};
                        if (csrfInput) {
                            headers['X-CSRF-TOKEN'] = csrfInput.value;
                        }
                        fetch('/api/tickets/' + ticket.id, { method: 'DELETE', headers: headers })
                            .then(function (response) {
                                if (!response.ok) {
                                    return response.json().catch(function () {
                                        return {};
                                    }).then(function (body) {
                                        throw new Error(body.error || Object.values(body)[0] || ('HTTP ' + response.status));
                                    });
                                }
                                modal.close();
                                window.location.reload();
                            })
                            .catch(function (err) {
                                window.showError('Αποτυχία διαγραφής ticket: ' + err.message);
                            });
                    });
                });
            }
        }
    }

    function openActionModal(config) {
        var form = document.getElementById('action-modal-form');
        var titleEl = document.getElementById('action-modal-title');
        var labelEl = document.getElementById('action-modal-comment-label');
        var textarea = document.getElementById('action-modal-comment');
        var submitBtn = document.getElementById('action-modal-submit');
        var csrfInput = document.getElementById('action-modal-csrf');
        var sourceCsrf = document.querySelector('#create-ticket-modal input[name="_csrf"]');

        form.setAttribute('action', config.action);
        titleEl.textContent = config.title;
        labelEl.textContent = config.label;
        textarea.value = '';
        textarea.placeholder = config.placeholder || '';
        setSubmitLabel(config.submitLabel || 'Επιβεβαίωση');
        submitBtn.disabled = false;
        csrfInput.value = sourceCsrf ? sourceCsrf.value : '';

        actionModal.showModal();
        textarea.focus();
    }

    // The confirm button is always a fixed ✓ glyph (tick/x pattern for
    // confirm/cancel); the actual action name goes into the tooltip instead.
    function setSubmitLabel(label) {
        var submitBtn = document.getElementById('action-modal-submit');
        submitBtn.title = label;
        submitBtn.setAttribute('aria-label', label);
    }

    function historyItemHtml(h) {
        var performer = escapeHtml(h.performedByUsername || 'Διαγραμμένος χρήστης');
        var assignee = escapeHtml(h.assignedToUsername || 'διαγραμμένο χρήστη');
        var dotClass = '';
        var text;

        switch (h.action) {
            case 'CREATED':
                text = 'Δημιουργήθηκε από ' + performer;
                break;
            case 'ASSIGNED':
                text = performer + ' ανέθεσε το ticket σε ' + assignee + '.';
                break;
            case 'REASSIGNED':
                text = performer + ' επανέθεσε το ticket σε ' + assignee + '.';
                break;
            case 'RESOLVED':
                text = performer + ' επέλυσε το ticket.';
                dotClass = 'dot-resolved';
                break;
            case 'CANCELLED':
                text = performer + ' ακύρωσε το ticket.';
                dotClass = 'dot-cancelled';
                break;
            case 'REOPENED':
                text = performer + ' επανάνοιξε το ticket.';
                break;
            default:
                text = performer + ' ενημέρωσε το ticket.';
        }

        var itemHtml = '<div class="timeline-item">';
        itemHtml += '<div class="timeline-dot ' + dotClass + '"></div>';
        itemHtml += '<p class="timeline-text">' + text + '</p>';
        itemHtml += '<p class="timeline-time">' + formatDate(h.timestamp) + '</p>';
        itemHtml += '</div>';
        return itemHtml;
    }

    function composeCommentHtml(ticketId) {
        var html = '<form id="td-comment-form" class="ticket-form comment-compose">';
        html += '<div class="form-group">';
        html += '<textarea name="commentText" placeholder="Γράψτε ένα σχόλιο…" required="required"></textarea>';
        html += '</div>';
        html += '<div class="ticket-action-buttons">';
        html += '<button type="submit" class="btn btn-success">Υποβολή</button>';
        html += '</div>';
        html += '</form>';
        return html;
    }

    function wireCommentCompose(ticketId) {
        var form = document.getElementById('td-comment-form');
        if (!form) {
            return;
        }
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var textarea = form.querySelector('textarea[name="commentText"]');
            var commentText = textarea.value.trim();
            if (!commentText) {
                return;
            }
            var submitBtn = form.querySelector('button[type="submit"]');
            submitBtn.disabled = true;

            var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
            var headers = { 'Content-Type': 'application/json' };
            if (csrfInput) {
                headers['X-CSRF-TOKEN'] = csrfInput.value;
            }

            fetch('/api/tickets/' + ticketId + '/comments', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({ commentText: commentText })
            }).then(function (response) {
                if (!response.ok) {
                    return response.json().catch(function () {
                        return {};
                    }).then(function (body) {
                        throw new Error(body.error || Object.values(body)[0] || ('HTTP ' + response.status));
                    });
                }
                return response.json();
            }).then(function () {
                openTicketModal(ticketId, 'td-tab-comments');
            }).catch(function (err) {
                submitBtn.disabled = false;
                window.showError('Αποτυχία προσθήκης σχολίου: ' + err.message);
            });
        });
    }

    function commentTabItemHtml(h, ticketId) {
        var performer = escapeHtml(h.performedByUsername || 'Διαγραμμένος χρήστης');
        var canEditComment = h.commentId && currentUserId && String(h.performedById) === currentUserId;

        var itemHtml = '<div class="timeline-item">';
        itemHtml += '<div class="timeline-dot dot-comment"></div>';
        itemHtml += '<p class="timeline-text">' + performer + ' σχολίασε:</p>';
        itemHtml += '<div class="timeline-quote-wrap" id="ct-quote-view-' + h.commentId + '">';
        itemHtml += '<p class="timeline-quote">“' + escapeHtml(h.commentText) + '”</p>';
        if (canEditComment) {
            itemHtml += '<button type="button" class="icon-btn comment-tab-edit-toggle" data-comment-id="' + h.commentId + '" title="Επεξεργασία σχολίου">✎</button>';
        }
        itemHtml += '</div>';
        if (canEditComment) {
            var csrfInputForComment = document.querySelector('#create-ticket-modal input[name="_csrf"]');
            var commentCsrfToken = csrfInputForComment ? csrfInputForComment.value : '';
            itemHtml += '<form id="ct-comment-edit-' + h.commentId + '" class="comment-edit-form" method="post" ' +
                'action="/tickets/' + ticketId + '/comments/' + h.commentId + '/edit" style="display:none;">';
            itemHtml += '<input type="hidden" name="_csrf" value="' + escapeHtml(commentCsrfToken) + '">';
            itemHtml += '<textarea name="text" required="required">' + escapeHtml(h.commentText) + '</textarea>';
            itemHtml += '<div class="ticket-action-buttons">';
            itemHtml += '<button type="button" class="btn btn-sm comment-tab-edit-cancel" data-comment-id="' + h.commentId + '">Άκυρο</button>';
            itemHtml += '<button type="submit" class="btn btn-primary btn-sm">Αποθήκευση</button>';
            itemHtml += '</div>';
            itemHtml += '</form>';
        }
        itemHtml += '<p class="timeline-time">' + formatDate(h.timestamp) + '</p>';
        itemHtml += '</div>';
        return itemHtml;
    }

    function wireCommentTabEdits() {
        document.querySelectorAll('.comment-tab-edit-toggle').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var commentId = btn.getAttribute('data-comment-id');
                document.getElementById('ct-quote-view-' + commentId).style.display = 'none';
                document.getElementById('ct-comment-edit-' + commentId).style.display = 'flex';
            });
        });
        document.querySelectorAll('.comment-tab-edit-cancel').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var commentId = btn.getAttribute('data-comment-id');
                document.getElementById('ct-comment-edit-' + commentId).style.display = 'none';
                document.getElementById('ct-quote-view-' + commentId).style.display = 'flex';
            });
        });
    }

    function attachmentsHtml(attachments) {
        if (!attachments || !attachments.length) {
            return '<p class="field-label">Δεν υπάρχουν συνημμένα.</p>';
        }
        var html = '<div class="attachment-list">';
        attachments.forEach(function (a) {
            var canDelete = currentUserId && a.uploadedById && String(a.uploadedById) === currentUserId;
            html += '<span class="attachment-chip">';
            html += '<a href="/api/attachments/' + a.id + '">📎 ' + escapeHtml(a.fileName) + '</a>';
            if (canDelete) {
                html += '<button type="button" class="attachment-delete" data-attachment-id="' + a.id + '" title="Αφαίρεση συνημμένου">✕</button>';
            }
            html += '</span>';
        });
        html += '</div>';
        return html;
    }

    function wireAttachments(ticketId) {
        var toggleBtn = document.getElementById('td-attachment-toggle');
        var input = document.getElementById('td-attachment-input');
        if (!toggleBtn || !input) {
            return;
        }

        toggleBtn.addEventListener('click', function () {
            input.click();
        });

        input.addEventListener('change', function () {
            if (!input.files || !input.files.length) {
                return;
            }
            var formData = new FormData();
            formData.append('file', input.files[0]);

            var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
            var headers = {};
            if (csrfInput) {
                formData.append('_csrf', csrfInput.value);
                headers['X-CSRF-TOKEN'] = csrfInput.value;
            }

            fetch('/api/tickets/' + ticketId + '/attachments', {
                method: 'POST',
                headers: headers,
                body: formData
            }).then(function (response) {
                if (!response.ok) {
                    return response.json().catch(function () {
                        return {};
                    }).then(function (body) {
                        throw new Error(body.error || Object.values(body)[0] || ('HTTP ' + response.status));
                    });
                }
                return response.json();
            }).then(function () {
                openTicketModal(ticketId);
            }).catch(function (err) {
                window.showError('Αποτυχία επισύναψης αρχείου: ' + err.message);
            });
        });

        document.querySelectorAll('.attachment-delete').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var attachmentId = btn.getAttribute('data-attachment-id');
                window.showConfirm('Αφαίρεση αυτού του συνημμένου;').then(function (ok) {
                    if (!ok) {
                        return;
                    }
                    var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
                    var headers = {};
                    if (csrfInput) {
                        headers['X-CSRF-TOKEN'] = csrfInput.value;
                    }
                    fetch('/api/attachments/' + attachmentId, {
                        method: 'DELETE',
                        headers: headers
                    }).then(function (response) {
                        if (!response.ok) {
                            return response.json().catch(function () {
                                return {};
                            }).then(function (body) {
                                throw new Error(body.error || Object.values(body)[0] || ('HTTP ' + response.status));
                            });
                        }
                        openTicketModal(ticketId);
                    }).catch(function (err) {
                        window.showError('Αποτυχία αφαίρεσης συνημμένου: ' + err.message);
                    });
                });
            });
        });
    }

    function formatRelativeTime(isoString) {
        if (!isoString) {
            return 'πριν από άγνωστο χρονικό διάστημα';
        }
        var then = new Date(isoString).getTime();
        if (isNaN(then)) {
            return 'πριν από άγνωστο χρονικό διάστημα';
        }
        var seconds = Math.floor((Date.now() - then) / 1000);
        if (seconds < 60) {
            return 'μόλις τώρα';
        }
        var units = [
            ['έτος', 'χρόνια', 31536000],
            ['μήνα', 'μήνες', 2592000],
            ['εβδομάδα', 'εβδομάδες', 604800],
            ['μέρα', 'μέρες', 86400],
            ['ώρα', 'ώρες', 3600],
            ['λεπτό', 'λεπτά', 60]
        ];
        for (var i = 0; i < units.length; i++) {
            var value = Math.floor(seconds / units[i][2]);
            if (value >= 1) {
                return value + ' ' + (value === 1 ? units[i][0] : units[i][1]) + ' πριν';
            }
        }
        return 'μόλις τώρα';
    }

    function formatDate(isoString) {
        if (!isoString) {
            return '—';
        }
        var d = new Date(isoString);
        if (isNaN(d.getTime())) {
            return isoString;
        }
        return d.toLocaleString();
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
});

document.addEventListener('DOMContentLoaded', function () {
    var createModal = document.getElementById('create-ticket-modal');
    if (createModal && createModal.getAttribute('data-has-errors') === 'true') {
        createModal.showModal();
    }

    // Shared open trigger for the toolbar button, the global "N" shortcut (wired in
    // common.js so it works from any page), and the ?openCreate=true redirect used
    // when the shortcut is pressed somewhere other than /tickets.
    if (createModal) {
        window.openCreateTicketModal = function () {
            createModal.showModal();
            var phoneInput = document.getElementById('ct-phone');
            if (phoneInput) {
                phoneInput.focus();
            }
        };

        if (new URLSearchParams(window.location.search).get('openCreate')) {
            window.openCreateTicketModal();
            var cleanCreateUrl = window.location.pathname + window.location.hash;
            window.history.replaceState(null, '', cleanCreateUrl);
        }

        wireArrowFieldNavigation(createModal.querySelector('form'));
    }

    // Shift+↓/↑ jumps focus to the next/previous field, following the visual
    // (top-to-bottom) order of the form — including textareas.
    function wireArrowFieldNavigation(form) {
        if (!form) {
            return;
        }
        form.addEventListener('keydown', function (e) {
            if (!e.shiftKey || (e.key !== 'ArrowDown' && e.key !== 'ArrowUp')) {
                return;
            }
            var target = e.target;

            var fields = Array.prototype.slice.call(form.querySelectorAll('input, select, textarea')).filter(function (el) {
                return el.type !== 'hidden' && !el.disabled && el.offsetParent !== null;
            });
            var idx = fields.indexOf(target);
            if (idx === -1) {
                return;
            }
            var nextIdx = e.key === 'ArrowDown' ? idx + 1 : idx - 1;
            if (nextIdx < 0 || nextIdx >= fields.length) {
                return;
            }
            e.preventDefault();
            fields[nextIdx].focus();
            if (typeof fields[nextIdx].select === 'function') {
                fields[nextIdx].select();
            }
        });
    }

    var categorySelect = document.getElementById('ct-category');
    var subcategorySelect = document.getElementById('ct-subcategory');

    if (!categorySelect || !subcategorySelect) {
        return;
    }

    categorySelect.addEventListener('change', function () {
        loadSubcategories(categorySelect.value, null);
    });

    var initialSubcategoryId = subcategorySelect.getAttribute('data-initial-value');
    if (categorySelect.value) {
        loadSubcategories(categorySelect.value, initialSubcategoryId);
    }

    function loadSubcategories(categoryId, selectedValue) {
        if (!categoryId) {
            subcategorySelect.innerHTML = '<option value="">Επιλέξτε πρώτα κατηγορία βλάβης</option>';
            subcategorySelect.disabled = true;
            return;
        }

        subcategorySelect.disabled = true;
        subcategorySelect.innerHTML = '<option value="">Φόρτωση…</option>';

        fetch('/api/subcategories?categoryId=' + encodeURIComponent(categoryId))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (subcategories) {
                var html = '<option value="">Προαιρετικό</option>';
                subcategories.forEach(function (sc) {
                    if (!sc.active && String(sc.id) !== String(selectedValue)) {
                        return;
                    }
                    html += '<option value="' + sc.id + '">' + escapeSubcategoryHtml(sc.name) + (sc.active ? '' : ' (Ανενεργή)') + '</option>';
                });
                subcategorySelect.innerHTML = html;
                if (selectedValue) {
                    subcategorySelect.value = selectedValue;
                }
                subcategorySelect.disabled = false;
            })
            .catch(function () {
                subcategorySelect.innerHTML = '<option value="">Αποτυχία φόρτωσης υποκατηγοριών</option>';
            });
    }

    function escapeSubcategoryHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
});

// Department combobox: a text input backed by the (hidden) #ct-department <select>
// so the user can type to filter, but the submitted value must match an existing option.
document.addEventListener('DOMContentLoaded', function () {
    var input = document.getElementById('ct-department-input');
    var select = document.getElementById('ct-department');
    var optionsList = document.getElementById('ct-department-options');

    if (!input || !select || !optionsList) {
        return;
    }

    var options = Array.prototype.slice.call(select.options)
        .filter(function (opt) { return opt.value; })
        .map(function (opt) { return { value: opt.value, label: opt.text }; });

    var activeIndex = -1;

    // Pre-fill from a server-rendered selection (e.g. re-showing the form after a validation error).
    if (select.value) {
        var selected = options.filter(function (o) { return o.value === select.value; })[0];
        if (selected) {
            input.value = selected.label;
        }
    }

    input.addEventListener('input', function () {
        select.value = '';
        renderOptions(input.value.trim().toLowerCase());
    });

    input.addEventListener('focus', function () {
        renderOptions(input.value.trim().toLowerCase());
    });

    input.addEventListener('keydown', function (e) {
        if (e.shiftKey) {
            return;
        }
        var items = optionsList.querySelectorAll('li');
        if (!items.length || optionsList.hidden) {
            return;
        }
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            activeIndex = Math.min(activeIndex + 1, items.length - 1);
            highlight(items);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            activeIndex = Math.max(activeIndex - 1, 0);
            highlight(items);
        } else if (e.key === 'Enter') {
            if (activeIndex >= 0 && items[activeIndex]) {
                e.preventDefault();
                choose(items[activeIndex].getAttribute('data-value'), items[activeIndex].textContent);
            }
        } else if (e.key === 'Escape') {
            closeOptions();
        }
    });

    input.addEventListener('blur', function () {
        // Delay so a click on an option registers before the list closes.
        setTimeout(function () {
            closeOptions();
            var current = options.filter(function (o) { return o.value === select.value; })[0];
            input.value = current ? current.label : '';
        }, 150);
    });

    function renderOptions(query) {
        var matches = options.filter(function (o) { return o.label.toLowerCase().indexOf(query) !== -1; });
        activeIndex = -1;
        if (!matches.length) {
            closeOptions();
            return;
        }
        optionsList.innerHTML = matches.map(function (o) {
            return '<li data-value="' + o.value + '">' + escapeSubcategoryHtml(o.label) + '</li>';
        }).join('');
        optionsList.hidden = false;
    }

    function highlight(items) {
        items.forEach(function (item, i) {
            item.classList.toggle('active', i === activeIndex);
        });
        if (items[activeIndex]) {
            items[activeIndex].scrollIntoView({ block: 'nearest' });
        }
    }

    function closeOptions() {
        optionsList.hidden = true;
        optionsList.innerHTML = '';
        activeIndex = -1;
    }

    function choose(value, label) {
        select.value = value;
        input.value = label;
        closeOptions();
    }

    optionsList.addEventListener('mousedown', function (e) {
        var li = e.target.closest('li');
        if (!li) {
            return;
        }
        e.preventDefault();
        choose(li.getAttribute('data-value'), li.textContent);
    });

    function escapeSubcategoryHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
});
