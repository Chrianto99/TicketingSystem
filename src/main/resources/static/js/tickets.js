document.addEventListener('DOMContentLoaded', function () {
    var STATUS_LABELS = { OPEN: 'Ανοιχτό', RESOLVED: 'Επιλύθηκε', CANCELLED: 'Ακυρώθηκε' };
    var PRIORITY_LABELS = { LOW: 'Χαμηλή', MEDIUM: 'Μεσαία', HIGH: 'Υψηλή' };
    var SOURCE_LABELS = { MANUAL: 'Χειροκίνητο', INCIDENT: 'Από Συμβάν', CALLBACK: 'Επιστροφή Κλήσης' };

    var modal = document.getElementById('ticket-modal');
    var modalTitle = document.getElementById('modal-title');
    var modalStatusBadge = document.getElementById('modal-status-badge');
    var modalPriorityBadge = document.getElementById('modal-priority-badge');
    var modalSourceBadge = document.getElementById('modal-source-badge');
    var modalMeta = document.getElementById('modal-meta');
    var modalBody = document.getElementById('modal-body');
    var modalFooter = document.getElementById('modal-footer');
    var currentUserId = document.body.getAttribute('data-current-user-id');
    var actionModal = document.getElementById('action-modal');
    var actionModalForm = document.getElementById('action-modal-form');

    // Comments/history are fetched lazily, the first time their tab is opened,
    // and then cached for the rest of this modal session — switching tabs back
    // and forth never re-fetches. Reset whenever the modal is (re)opened.
    var currentTicketId = null;
    var commentsLoaded = false;
    var historyLoaded = false;

    if (!modal) {
        return;
    }

    // Live push for the "My Tickets" red dot — the dot's initial state is
    // already server-rendered from the DB flag; this just lights it up without
    // a reload if a ticket gets (re)assigned to this user while the page is open.
    if (window.EventSource && currentUserId) {
        var notificationSource = new EventSource('/api/notifications/stream');
        notificationSource.addEventListener('ticket-assigned', function () {
            var dot = document.getElementById('my-tickets-dot');
            if (dot) {
                dot.classList.add('visible');
            }
        });
    }

    modal.addEventListener('click', function (e) {
        if (e.target === modal) {
            modal.close();
        }
    });

    // Delegated so it keeps working across every re-render of modalBody's innerHTML.
    modalBody.addEventListener('click', function (e) {
        var toggle = e.target.closest('.change-lines-toggle');
        if (!toggle) {
            return;
        }
        var container = toggle.closest('.timeline-changes');
        if (!container) {
            return;
        }
        container.querySelectorAll('.change-line-extra').forEach(function (el) {
            el.hidden = false;
        });
        toggle.remove();
    });

    // Live search (title, department, category, subcategory, assignee username):
    // auto-submits the filters form (carrying over every other active filter)
    // shortly after the user stops typing, instead of requiring an explicit search button.
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
        document.getElementById('action-modal-suggestion').addEventListener('click', function () {
            var textarea = document.getElementById('action-modal-comment');
            textarea.value = 'Επιλύθηκε.';
            textarea.focus();
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
        modalSourceBadge.style.display = 'none';
        modalMeta.textContent = '';
        modalBody.innerHTML = '<p>Φόρτωση…</p>';
        modalFooter.innerHTML = '';
        modalFooter.style.display = 'none';
        var headerMenu = document.getElementById('ticket-modal-menu');
        headerMenu.hidden = true;
        headerMenu.removeAttribute('open');
        modal.showModal();

        currentTicketId = ticketId;
        commentsLoaded = false;
        historyLoaded = false;

        Promise.all([
            fetch('/api/tickets/' + ticketId).then(handleResponse),
            fetch('/api/tickets/' + ticketId + '/attachments').then(handleResponse)
        ]).then(function (results) {
            renderTicket(results[0], results[1]);
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

    function renderTicket(ticket, attachments) {
        modalTitle.textContent = 'Ticket #' + ticket.id;
        modalStatusBadge.textContent = STATUS_LABELS[ticket.status] || ticket.status;
        modalStatusBadge.className = 'badge status-' + ticket.status;
        modalStatusBadge.style.display = '';
        if (ticket.priority) {
            modalPriorityBadge.textContent = PRIORITY_LABELS[ticket.priority] || ticket.priority;
            modalPriorityBadge.className = 'badge priority-' + ticket.priority;
            modalPriorityBadge.style.display = '';
        } else {
            modalPriorityBadge.style.display = 'none';
        }
        modalSourceBadge.textContent = SOURCE_LABELS[ticket.source] || ticket.source;
        modalSourceBadge.className = 'badge source-' + ticket.source;
        modalSourceBadge.style.display = '';
        modalMeta.textContent = 'Δημιουργήθηκε από ' + (ticket.creatorUsername || 'διαγραμμένο χρήστη') + ' ' + formatRelativeTime(ticket.createdAt);

        var html = '';
        // Callback tickets are complete-or-delete, never have a history worth
        // reviewing, and aren't expected to collect comments/attachments — so they
        // skip the tab bar entirely and just show the info panel.
        var isCallback = ticket.source === 'CALLBACK';

        if (!isCallback) {
            html += '<div class="modal-tabs" role="tablist">';
            html += '<button type="button" class="modal-tab active" data-tab-target="td-tab-info" role="tab" aria-selected="true">Πληροφορίες</button>';
            html += '<button type="button" class="modal-tab" data-tab-target="td-tab-comments" role="tab" aria-selected="false">Σχόλια</button>';
            html += '<button type="button" class="modal-tab" data-tab-target="td-tab-history" role="tab" aria-selected="false">Ιστορικό</button>';
            html += '</div>';
        }

        html += '<div id="td-tab-info" class="modal-tab-panel">';
        html += '<h2 class="ticket-summary-title">' + escapeHtml(ticket.summary || '—') + '</h2>';
        html += '<div id="info-fields">' + infoFieldsHtml(ticket, attachments) + '</div>';
        html += '</div>';

        if (!isCallback) {
            // History and Comments are fetched on first activation (see activateTab)
            // rather than up front — these two panels start empty.
            html += '<div id="td-tab-history" class="modal-tab-panel" style="display:none;"></div>';

            html += '<div id="td-tab-comments" class="modal-tab-panel" style="display:none;">';
            if (ticket.status === 'OPEN') {
                html += composeCommentHtml(ticket.id);
            }
            html += '<div id="comments-list"></div>';
            html += '</div>';
        }

        modalBody.innerHTML = html;
        wireReassign();
        wireResolveForm(ticket);
        wireCommentCompose(ticket.id);
        wireModalTabs();
        wireAttachments(ticket.id);
        wireHeaderMenu(ticket, attachments);

        modalFooter.innerHTML = actionAreaHtml(ticket);
        modalFooter.style.display = '';
        wireActionButtons(ticket);
    }

    // Fetched once per modal-open and cached in commentsLoaded/historyLoaded —
    // switching tabs back and forth re-shows the cached render, it never re-fetches.
    function loadHistory(ticketId) {
        var panel = document.getElementById('td-tab-history');
        panel.innerHTML = '<p class="field-label" style="text-align:center;">Φόρτωση…</p>';
        fetch('/api/tickets/' + ticketId + '/history').then(handleResponse).then(function (history) {
            historyLoaded = true;
            if (!history.length) {
                panel.innerHTML = '<p class="field-label" style="text-align:center;">Δεν υπάρχει ιστορικό ακόμα.</p>';
                return;
            }
            var html = '<div class="timeline">';
            history.forEach(function (h) {
                html += historyItemHtml(h);
            });
            html += '</div>';
            panel.innerHTML = html;
        }).catch(function () {
            panel.innerHTML = '<p class="field-label" style="text-align:center;">Αποτυχία φόρτωσης ιστορικού.</p>';
        });
    }

    function loadComments(ticketId) {
        var listContainer = document.getElementById('comments-list');
        listContainer.innerHTML = '<p class="field-label" style="text-align:center;">Φόρτωση…</p>';
        fetch('/api/tickets/' + ticketId + '/comments').then(handleResponse).then(function (comments) {
            commentsLoaded = true;
            if (!comments.length) {
                listContainer.innerHTML = '<p class="field-label" style="text-align:center;">Δεν υπάρχουν σχόλια ακόμα.</p>';
            } else {
                var html = '<div class="timeline">';
                comments.forEach(function (c) {
                    html += commentTabItemHtml(c, ticketId);
                });
                html += '</div>';
                listContainer.innerHTML = html;
            }
            wireCommentTabEdits(ticketId);
        }).catch(function () {
            listContainer.innerHTML = '<p class="field-label" style="text-align:center;">Αποτυχία φόρτωσης σχολίων.</p>';
        });
    }

    // "⋮" header menu: Επεξεργασία (edit info, any logged-in user) and Ακύρωση Ticket
    // (only while OPEN — matches the same rules the backend already enforces).
    function wireHeaderMenu(ticket, attachments) {
        var menu = document.getElementById('ticket-modal-menu');
        var editBtn = document.getElementById('ticket-menu-edit-btn');
        var cancelBtn = document.getElementById('ticket-menu-cancel-btn');
        var divider = menu.querySelector('.entity-menu-divider');

        // Editing pulls its department/category/priority dropdown options by
        // cloning #ct-* from the create-ticket form, which only exists on the
        // Tickets page — so the edit action is unavailable wherever this modal
        // is embedded without that form (e.g. the incident detail page).
        var canEdit = !!currentUserId && !!document.querySelector('#ct-assignee option');
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
            modalFooter.style.display = 'none';
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

    // Incident-derived tickets (source === 'INCIDENT') never collected caller/phone/
    // department/category — those inputs don't exist on IncidentTicketCreateRequest —
    // so they get a different field set instead of showing empty manual-ticket fields.
    // Assignee/resolution/attachments are identical either way and stay factored out
    // so wireReassign()/wireResolveForm()/wireAttachments() keep working unchanged.
    function infoFieldsHtml(ticket, attachments) {
        var html = '';

        if (ticket.source === 'INCIDENT') {
            html += fieldBlock('Δημιουργός', escapeHtml(ticket.creatorUsername || 'διαγραμμένο χρήστη'));

            var incidentLabel = ticket.incidentSubject || ('Συμβάν #' + ticket.incidentId);
            var incidentValue = ticket.incidentId
                ? '<a href="/incidents/' + ticket.incidentId + '">' + escapeHtml(incidentLabel) + '</a>'
                : '—';
            html += fieldBlock('Σχετικό Συμβάν', incidentValue);
        } else if (ticket.source === 'CALLBACK') {
            html += fieldBlock('Όνομα καλούντος', escapeHtml(ticket.callerName || '—'));
            html += fieldBlock('Αριθμός τηλεφώνου', escapeHtml(ticket.phoneNumber || '—'));
        } else {
            html += fieldBlock('Όνομα καλούντος', escapeHtml(ticket.callerName || '—'));
            html += fieldBlock('Αριθμός τηλεφώνου', escapeHtml(ticket.phoneNumber || '—'));
            html += fieldBlock('Τμήμα', escapeHtml(ticket.departmentName || '—'));
            if (ticket.ipAddress) {
                html += fieldBlock('Διεύθυνση IP', escapeHtml(ticket.ipAddress));
            }

            var categoryValue = ticket.category
                ? ticket.category + (ticket.subcategory ? ' → ' + ticket.subcategory : '')
                : '—';
            html += fieldBlock('Κατηγορία Βλάβης', escapeHtml(categoryValue));
        }

        html += assigneeFieldHtml(ticket);
        html += fieldBlock('Λεπτομέρειες', escapeHtml(ticket.description || '—'));
        // Callback tickets skip Λύση — there's no resolve-with-text flow (see the
        // footer action button instead, which deletes the ticket outright).
        if (ticket.source !== 'CALLBACK') {
            html += resolutionFieldHtml(ticket);
        }

        if (ticket.source === 'INCIDENT') {
            html += fieldBlock('Δημιουργήθηκε', formatDate(ticket.createdAt));
            html += fieldBlock('Ενημερώθηκε', formatDate(ticket.updatedAt));
        }

        if (ticket.source !== 'CALLBACK') {
            html += attachmentsFieldHtml(attachments);
        }

        return html;
    }

    function assigneeFieldHtml(ticket) {
        // Same #ct-assignee dependency as the edit-info form (see wireHeaderMenu) —
        // the reassign dropdown clones its options from there, so the action only
        // makes sense wherever that source select actually exists.
        var canReassign = ticket.status === 'OPEN' && !!document.querySelector('#ct-assignee option');

        var assigneeValue = '<div class="assignee-view" id="assignee-view">';
        assigneeValue += '<span id="td-assignee">' + escapeHtml(ticket.assignedUsername || 'Χωρίς ανάθεση') + '</span>';
        if (canReassign) {
            assigneeValue += '<button type="button" class="btn btn-sm" id="td-reassign-toggle">Ανάθεση σε</button>';
        }
        assigneeValue += '</div>';
        var html = fieldBlock('Ανατέθηκε σε', assigneeValue);

        if (canReassign) {
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
        return html;
    }

    function resolutionFieldHtml(ticket) {
        var html = '<div class="field-label" style="margin-top:10px;">Λύση</div>';
        html += '<div class="description-block" id="resolution-view">' + escapeHtml(ticket.resolution || '—') + '</div>';
        if (ticket.status === 'OPEN') {
            html += '<form id="td-resolve-form" class="resolve-edit" style="display:none;">';
            html += '<textarea id="td-resolve-textarea" placeholder="Περιγράψτε τη λύση…">' + escapeHtml(ticket.resolution || '') + '</textarea>';
            html += '<button type="button" class="resolution-suggestion" id="td-resolve-suggestion">✓ Επιλύθηκε</button>';
            if (!ticket.subcategoryId && ticket.source !== 'INCIDENT') {
                html += '<select id="td-resolve-subcategory"><option value="">Φόρτωση…</option></select>';
            }
            html += '<div class="ticket-action-buttons">';
            html += '<button type="button" class="btn btn-icon-only" id="td-resolve-cancel" title="Άκυρο" aria-label="Άκυρο">✗</button>';
            html += '<button type="submit" class="btn btn-primary btn-icon-only" id="td-resolve-submit" title="Επιβεβαίωση" aria-label="Επιβεβαίωση">✓</button>';
            html += '</div>';
            html += '</form>';
        }
        return html;
    }

    function attachmentsFieldHtml(attachments) {
        var html = '<div class="field-label attachments-heading" style="margin-top:10px;">';
        html += '<span>Επισυναπτόμενα</span>';
        html += '<button type="button" class="icon-btn attachment-toggle-btn" id="td-attachment-toggle" title="Επισύναψη αρχείου" aria-label="Επισύναψη αρχείου">';
        html += '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5c0-1.38 1.12-2.5 2.5-2.5s2.5 1.12 2.5 2.5v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5c0 1.38 1.12 2.5 2.5 2.5s2.5-1.12 2.5-2.5V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z"/></svg>';
        html += '</button>';
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
        html += '<label for="td-edit-description">Λεπτομέρειες (προαιρετικό)</label>';
        html += '<textarea id="td-edit-description" name="description">' + escapeHtml(ticket.description || '') + '</textarea>';
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
            modalFooter.style.display = '';
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
        var suggestionBtn = document.getElementById('td-resolve-suggestion');

        if (subcatSelect) {
            loadResolveSubcategories(ticket.categoryId);
        }

        suggestionBtn.addEventListener('click', function () {
            textarea.value = 'Επιλύθηκε.';
            textarea.focus();
        });

        cancelBtn.addEventListener('click', function () {
            form.style.display = 'none';
            view.style.display = '';
            modalFooter.style.display = '';
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
                window.location.reload();
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

        if (tabTarget === 'td-tab-comments' && !commentsLoaded) {
            loadComments(currentTicketId);
        }
        if (tabTarget === 'td-tab-history' && !historyLoaded) {
            loadHistory(currentTicketId);
        }
    }

    function actionAreaHtml(ticket) {
        var isOpen = ticket.status === 'OPEN';
        var html = '<div class="ticket-action-area">';

        if (isOpen) {
            html += '<div class="ticket-action-buttons ticket-action-buttons-center">';
            if (ticket.source === 'CALLBACK') {
                html += '<button type="button" class="btn btn-primary" id="ticket-resolve-callback-btn">✓ Ολοκλήρωση Callback</button>';
            } else {
                html += '<button type="button" class="btn btn-primary" id="ticket-resolve-btn">Επίλυση Ticket</button>';
            }
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

        if (isOpen && ticket.source === 'CALLBACK') {
            document.getElementById('ticket-resolve-callback-btn').addEventListener('click', function () {
                window.showConfirm('Ολοκλήρωση αυτού του callback; Το ticket θα διαγραφεί οριστικά — δεν χρειάζεται λύση.').then(function (ok) {
                    if (!ok) {
                        return;
                    }
                    var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
                    var headers = {};
                    if (csrfInput) {
                        headers['X-CSRF-TOKEN'] = csrfInput.value;
                    }
                    fetch('/api/tickets/' + ticket.id + '/resolve-callback', { method: 'PATCH', headers: headers })
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
                            window.showError('Αποτυχία ολοκλήρωσης callback: ' + err.message);
                        });
                });
            });
        } else if (isOpen) {
            document.getElementById('ticket-resolve-btn').addEventListener('click', function () {
                activateTab('td-tab-info');
                document.getElementById('resolution-view').style.display = 'none';
                document.getElementById('td-resolve-form').style.display = 'flex';
                document.getElementById('td-resolve-textarea').focus();
                modalFooter.style.display = 'none';
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

    // Icon + tint shown per history action, so the timeline reads at a glance
    // instead of relying on same-colored dots that get confused with the comments tab.
    // Grouped by category rather than one icon per action (assign/reassign share
    // one icon, any kind of edit shares the pencil) so the set stays small.
    // Same paperclip glyph as the attachments panel's "Επισύναψη αρχείου" button.
    var ICON_ATTACHMENT = '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5c0-1.38 1.12-2.5 2.5-2.5s2.5 1.12 2.5 2.5v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5c0 1.38 1.12 2.5 2.5 2.5s2.5-1.12 2.5-2.5V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z"/></svg>';
    var ICON_COMMENT = '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>';

    var HISTORY_ICONS = {
        CREATED: { icon: '+', cls: 'icon-created' },
        ASSIGNED: { icon: '➤', cls: 'icon-assigned' },
        REASSIGNED: { icon: '➤', cls: 'icon-assigned' },
        RESOLVED: { icon: '✓', cls: 'icon-resolved' },
        CANCELLED: { icon: '✗', cls: 'icon-cancelled' },
        REOPENED: { icon: '↺', cls: 'icon-reopened' },
        INFO_CHANGED: { icon: '✎', cls: 'icon-edited' },
        COMMENT_EDITED: { icon: '✎', cls: 'icon-edited' },
        COMMENT_ADDED: { icon: ICON_COMMENT, cls: 'icon-edited' },
        COMMENT_REMOVED: { icon: '✗', cls: 'icon-cancelled' },
        ATTACHMENT_ADDED: { icon: ICON_ATTACHMENT, cls: 'icon-edited' },
        ATTACHMENT_REMOVED: { icon: '✗', cls: 'icon-cancelled' }
    };

    function historyItemHtml(h) {
        var performer = escapeHtml(h.performedByUsername || 'Διαγραμμένος χρήστης');
        var text;

        switch (h.action) {
            case 'CREATED':
                text = 'Δημιουργήθηκε από ' + performer;
                break;
            case 'ASSIGNED':
            case 'REASSIGNED':
                // description is the full pre-composed sentence (performer +
                // verb + assignee, plus an optional reason) — no separate
                // assignedTo field exists anymore.
                text = escapeHtml(h.description || (performer + ' ενημέρωσε την ανάθεση.'));
                break;
            case 'RESOLVED':
                text = performer + ' επέλυσε το ticket.';
                break;
            case 'CANCELLED':
                text = performer + ' ακύρωσε το ticket.';
                break;
            case 'REOPENED':
                text = performer + ' επανάνοιξε το ticket.';
                break;
            case 'INFO_CHANGED':
                text = performer + ' επεξεργάστηκε τις πληροφορίες του ticket.';
                break;
            case 'COMMENT_EDITED':
                text = performer + ' επεξεργάστηκε ένα σχόλιο.';
                break;
            case 'COMMENT_ADDED':
                text = performer + ' πρόσθεσε ένα σχόλιο.';
                break;
            case 'COMMENT_REMOVED':
                text = performer + ' διέγραψε ένα σχόλιο.';
                break;
            case 'ATTACHMENT_ADDED':
                text = performer + ' επισύναψε ένα αρχείο.';
                break;
            case 'ATTACHMENT_REMOVED':
                text = performer + ' αφαίρεσε ένα αρχείο.';
                break;
            default:
                text = performer + ' ενημέρωσε το ticket.';
        }

        var iconInfo = HISTORY_ICONS[h.action] || { icon: '•', cls: 'icon-default' };

        var itemHtml = '<div class="timeline-item">';
        itemHtml += '<div class="timeline-icon ' + iconInfo.cls + '">' + iconInfo.icon + '</div>';
        itemHtml += '<p class="timeline-text">' + text + '</p>';
        if (h.description && h.action !== 'ASSIGNED' && h.action !== 'REASSIGNED') {
            itemHtml += h.action === 'INFO_CHANGED'
                ? infoChangeLinesHtml(h.description)
                : '<p class="timeline-quote">' + escapeHtml(h.description) + '</p>';
        }
        itemHtml += '<p class="timeline-time">' + formatDate(h.timestamp) + '</p>';
        itemHtml += '</div>';
        return itemHtml;
    }

    // INFO_CHANGED descriptions are built server-side as "Field: «old» → «new»"
    // entries joined with "; " — split them back out so each changed field gets
    // its own line, collapsing anything past the first 3 behind a "show" toggle
    // (see the delegated click handler on modalBody, below).
    var INFO_CHANGE_LINES_VISIBLE = 3;

    function infoChangeLinesHtml(description) {
        var lines = description.split('; ');
        var html = '<div class="timeline-quote timeline-changes">';
        lines.forEach(function (line, i) {
            var isExtra = i >= INFO_CHANGE_LINES_VISIBLE;
            html += '<div class="change-line' + (isExtra ? ' change-line-extra' : '') + '"' + (isExtra ? ' hidden' : '') + '>' +
                escapeHtml(line) + '</div>';
        });
        if (lines.length > INFO_CHANGE_LINES_VISIBLE) {
            html += '<button type="button" class="change-lines-toggle">Εμφάνιση (+' + (lines.length - INFO_CHANGE_LINES_VISIBLE) + ')</button>';
        }
        html += '</div>';
        return html;
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

    function commentTabItemHtml(c, ticketId) {
        var performer = escapeHtml(c.authorUsername || 'Διαγραμμένος χρήστης');
        var canEditComment = c.id && currentUserId && String(c.authorId) === currentUserId;

        var itemHtml = '<div class="timeline-item timeline-item-plain">';
        itemHtml += '<p class="timeline-text">' + performer + ' σχολίασε:</p>';
        itemHtml += '<div class="timeline-quote-wrap" id="ct-quote-view-' + c.id + '">';
        itemHtml += '<p class="timeline-quote">“' + escapeHtml(c.text) + '”</p>';
        if (canEditComment) {
            itemHtml += '<button type="button" class="icon-btn comment-tab-edit-toggle" data-comment-id="' + c.id + '" title="Επεξεργασία σχολίου">✎</button>';
            itemHtml += '<button type="button" class="icon-btn comment-tab-delete" data-comment-id="' + c.id + '" title="Διαγραφή σχολίου">🗑</button>';
        }
        itemHtml += '</div>';
        if (canEditComment) {
            var csrfInputForComment = document.querySelector('#create-ticket-modal input[name="_csrf"]');
            var commentCsrfToken = csrfInputForComment ? csrfInputForComment.value : '';
            itemHtml += '<form id="ct-comment-edit-' + c.id + '" class="comment-edit-form" method="post" ' +
                'action="/tickets/' + ticketId + '/comments/' + c.id + '/edit" style="display:none;">';
            itemHtml += '<input type="hidden" name="_csrf" value="' + escapeHtml(commentCsrfToken) + '">';
            itemHtml += '<textarea name="text" required="required">' + escapeHtml(c.text) + '</textarea>';
            itemHtml += '<div class="ticket-action-buttons">';
            itemHtml += '<button type="button" class="btn btn-sm comment-tab-edit-cancel" data-comment-id="' + c.id + '">Άκυρο</button>';
            itemHtml += '<button type="submit" class="btn btn-primary btn-sm">Αποθήκευση</button>';
            itemHtml += '</div>';
            itemHtml += '</form>';
        }
        itemHtml += '<p class="timeline-time">' + formatDate(c.timestamp) + '</p>';
        itemHtml += '</div>';
        return itemHtml;
    }

    function wireCommentTabEdits(ticketId) {
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
        document.querySelectorAll('.comment-tab-delete').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var commentId = btn.getAttribute('data-comment-id');
                window.showConfirm('Διαγραφή αυτού του σχολίου;').then(function (ok) {
                    if (!ok) {
                        return;
                    }
                    var csrfInput = document.querySelector('#create-ticket-modal input[name="_csrf"]');
                    var headers = {};
                    if (csrfInput) {
                        headers['X-CSRF-TOKEN'] = csrfInput.value;
                    }
                    fetch('/api/tickets/' + ticketId + '/comments/' + commentId, {
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
                        openTicketModal(ticketId, 'td-tab-comments');
                    }).catch(function (err) {
                        window.showError('Αποτυχία διαγραφής σχολίου: ' + err.message);
                    });
                });
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

    if (createModal) {
        createModal.addEventListener('click', function (e) {
            if (e.target === createModal) {
                createModal.close();
            }
        });
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
