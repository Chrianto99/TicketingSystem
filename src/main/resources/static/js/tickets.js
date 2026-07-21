document.addEventListener('DOMContentLoaded', function () {
    var modal = document.getElementById('ticket-modal');
    var modalTitle = document.getElementById('modal-title');
    var modalBody = document.getElementById('modal-body');

    if (!modal) {
        return;
    }

    document.querySelectorAll('.ticket-row').forEach(function (row) {
        row.addEventListener('click', function () {
            openTicketModal(row.getAttribute('data-ticket-id'));
        });
    });

    function openTicketModal(ticketId) {
        modalTitle.textContent = 'Ticket #' + ticketId;
        modalBody.innerHTML = '<p>Loading…</p>';
        modal.showModal();

        Promise.all([
            fetch('/api/tickets/' + ticketId).then(handleResponse),
            fetch('/api/tickets/' + ticketId + '/history').then(handleResponse)
        ]).then(function (results) {
            renderTicket(results[0], results[1]);
        }).catch(function (err) {
            modalBody.innerHTML = '<p class="modal-error">Failed to load ticket: ' + escapeHtml(err.message) + '</p>';
        });
    }

    function handleResponse(response) {
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        return response.json();
    }

    function renderTicket(ticket, history) {
        modalTitle.textContent = 'Ticket #' + ticket.id;

        var html = '';
        html += badgeFieldRow('Status', 'status-' + ticket.status, ticket.status);
        html += fieldRow('Summary', ticket.description);
        html += fieldRowPair('Caller name', ticket.callerName, 'Phone number', ticket.phoneNumber);
        html += fieldRowPair('Department', ticket.departmentName, 'Problem type', ticket.problemType);
        html += '<div class="field-row-pair">' +
            fieldRow('IP address', ticket.ipAddress) +
            badgeFieldRow('Priority', 'priority-' + ticket.priority, ticket.priority) +
            '</div>';
        html += fieldRow('Assigned to', ticket.assignedUsername || 'Unassigned');
        html += fieldRow('Created by', ticket.creatorUsername);
        html += fieldRow('Created', formatDate(ticket.createdAt));
        html += fieldRow('Updated', formatDate(ticket.updatedAt));

        html += '<h3>History</h3>';
        if (!history.length) {
            html += '<p class="field-label">No history yet.</p>';
        } else {
            history.forEach(function (h) {
                html += '<div class="history-item">';
                html += '<strong>' + escapeHtml(h.action) + '</strong> by ' + escapeHtml(h.performedByUsername || 'a deleted user');
                if (h.assignedToUsername) {
                    html += ' &rarr; assigned to ' + escapeHtml(h.assignedToUsername);
                }
                html += '<div class="history-meta">' + formatDate(h.timestamp) + '</div>';
                if (h.commentText) {
                    html += '<div class="description-block">' + escapeHtml(h.commentText) + '</div>';
                }
                html += '</div>';
            });
        }

        modalBody.innerHTML = html;
    }

    function fieldRow(label, value) {
        return '<div class="field-row"><span class="field-label">' + escapeHtml(label) + '</span><span>' +
            escapeHtml(value || '—') + '</span></div>';
    }

    function fieldRowPair(label1, value1, label2, value2) {
        return '<div class="field-row-pair">' + fieldRow(label1, value1) + fieldRow(label2, value2) + '</div>';
    }

    function badgeFieldRow(label, badgeClass, value) {
        return '<div class="field-row"><span class="field-label">' + escapeHtml(label) + '</span>' +
            '<span class="badge ' + badgeClass + '">' + escapeHtml(value) + '</span></div>';
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
