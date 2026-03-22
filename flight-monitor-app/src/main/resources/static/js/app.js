/* Flight Monitor — Main JavaScript */

document.addEventListener('DOMContentLoaded', function () {
    // Uppercase IATA inputs
    document.querySelectorAll('input[pattern="[A-Z]{3}"]').forEach(function (input) {
        input.addEventListener('input', function () {
            this.value = this.value.toUpperCase().replace(/[^A-Z]/g, '');
        });
    });

    // Sortable table columns
    document.querySelectorAll('th.sortable').forEach(function (th) {
        th.addEventListener('click', function () {
            const table = th.closest('table');
            const tbody = table.querySelector('tbody');
            const rows = Array.from(tbody.querySelectorAll('tr'));
            const col = Array.from(th.parentElement.children).indexOf(th);
            const asc = th.dataset.sortDir !== 'asc';
            th.dataset.sortDir = asc ? 'asc' : 'desc';

            rows.sort(function (a, b) {
                const aText = a.children[col].textContent.trim();
                const bText = b.children[col].textContent.trim();
                const aNum = parseFloat(aText.replace(/[^0-9.]/g, ''));
                const bNum = parseFloat(bText.replace(/[^0-9.]/g, ''));
                if (!isNaN(aNum) && !isNaN(bNum)) {
                    return asc ? aNum - bNum : bNum - aNum;
                }
                return asc ? aText.localeCompare(bText) : bText.localeCompare(aText);
            });

            rows.forEach(function (row) {
                tbody.appendChild(row);
            });
        });
    });
});
