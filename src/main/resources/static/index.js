document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const shortenForm = document.getElementById('shortenForm');
    const submitBtn = document.getElementById('submitBtn');
    const errorAlert = document.getElementById('errorAlert');
    const errorMessage = document.getElementById('errorMessage');
    
    // Result elements
    const resultCard = document.getElementById('resultCard');
    const resultEmpty = document.getElementById('resultEmpty');
    const resultActive = document.getElementById('resultActive');
    const resultShortUrl = document.getElementById('resultShortUrl');
    const copyBtn = document.getElementById('copyBtn');
    const visitBtn = document.getElementById('visitBtn');
    const qrCodeImg = document.getElementById('qrCodeImg');
    const downloadQrBtn = document.getElementById('downloadQrBtn');
    const detailOriginalUrl = document.getElementById('detailOriginalUrl');
    const detailAlias = document.getElementById('detailAlias');
    const detailExpiry = document.getElementById('detailExpiry');

    // KPI stats elements
    const statTotalUrls = document.getElementById('statTotalUrls');
    const statTotalClicks = document.getElementById('statTotalClicks');
    const statMostClicked = document.getElementById('statMostClicked');
    const statMostClickedClicks = document.getElementById('statMostClickedClicks');

    // Table elements
    const linksTableBody = document.querySelector('#linksTable tbody');
    const tableSearchInput = document.getElementById('tableSearchInput');

    // Global variables for Chart and Table
    let clickChart = null;
    let localLinksList = [];

    // Initialize App Data
    refreshDashboard();

    // Form submission
    shortenForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // Reset states
        errorAlert.classList.add('hidden');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span>Processing...</span> <i class="fa-solid fa-spinner fa-spin"></i>';

        const originalUrl = document.getElementById('originalUrl').value;
        const customAlias = document.getElementById('customAlias').value.trim() || null;
        const expiryDaysVal = document.getElementById('expiryDays').value;
        const expiryDays = expiryDaysVal ? parseInt(expiryDaysVal, 10) : null;

        try {
            const response = await fetch('/api/shorten', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ originalUrl, customAlias, expiryDays })
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || 'Failed to shorten URL. Make sure it is valid.');
            }

            // Populate active result
            resultShortUrl.value = data.shortUrl;
            visitBtn.href = data.shortUrl;
            
            const qrUrl = `/api/urls/${data.shortCode}/qrcode`;
            qrCodeImg.src = qrUrl;
            downloadQrBtn.href = qrUrl;

            detailOriginalUrl.textContent = data.originalUrl;
            detailAlias.textContent = data.customAlias ? data.customAlias : data.shortCode;
            detailExpiry.textContent = data.expiresAt ? formatDate(data.expiresAt) : 'Permanent Link';

            // Show active result view
            resultCard.classList.remove('empty-state');
            resultEmpty.classList.add('hidden');
            resultActive.classList.remove('hidden');
            
            // Clear inputs
            document.getElementById('originalUrl').value = '';
            document.getElementById('customAlias').value = '';
            document.getElementById('expiryDays').value = '';

            // Refresh statistics and data tables
            refreshDashboard();

        } catch (err) {
            errorMessage.textContent = err.message;
            errorAlert.classList.remove('hidden');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<span>Shorten Link</span> <i class="fa-solid fa-arrow-right"></i>';
        }
    });

    // Copy Button Action
    copyBtn.addEventListener('click', () => {
        const textToCopy = resultShortUrl.value;
        navigator.clipboard.writeText(textToCopy).then(() => {
            const originalHTML = copyBtn.innerHTML;
            copyBtn.innerHTML = '<i class="fa-solid fa-check"></i>';
            copyBtn.style.background = 'var(--accent-color)';
            
            setTimeout(() => {
                copyBtn.innerHTML = originalHTML;
                copyBtn.style.background = '';
            }, 2000);
        }).catch(err => console.error('Copy failed: ', err));
    });

    // Client-side table search filter
    tableSearchInput.addEventListener('input', (e) => {
        const searchTerm = e.target.value.toLowerCase().trim();
        renderTable(searchTerm);
    });

    // Refresh Dashboard statistics, charts, and tables
    async function refreshDashboard() {
        await fetchStats();
        await fetchTableList();
    }

    // Fetch Stats Summary and Render Traffic Chart
    async function fetchStats() {
        try {
            const response = await fetch('/api/stats');
            if (!response.ok) return;
            const data = await response.json();

            // Populate KPI elements
            statTotalUrls.textContent = data.totalUrls;
            statTotalClicks.textContent = data.totalClicks;
            
            if (data.mostClickedUrl) {
                statMostClicked.textContent = data.mostClickedUrl.customAlias || data.mostClickedUrl.shortCode;
                statMostClicked.href = data.mostClickedUrl.shortUrl;
                statMostClickedClicks.textContent = `${data.mostClickedUrl.clickCount} Clicks`;
            } else {
                statMostClicked.textContent = 'None';
                statMostClickedClicks.textContent = '0 Clicks';
            }

            // Draw Chart.js hourly redirection traffic graph
            renderChart(data.hourlyClicks);

        } catch (err) {
            console.error('Error fetching statistics: ', err);
        }
    }

    // Fetch Links list for manager table
    async function fetchTableList() {
        try {
            // Fetch size 100 to show all links in management console
            const response = await fetch('/api/urls?page=0&size=100&sort=createdAt,desc');
            if (!response.ok) return;
            const data = await response.json();
            
            localLinksList = data.content || [];
            renderTable('');

        } catch (err) {
            console.error('Error fetching table items: ', err);
        }
    }

    // Render Table items with optional search query filter
    function renderTable(filterQuery) {
        linksTableBody.innerHTML = '';
        
        const filtered = localLinksList.filter(link => {
            const code = link.shortCode.toLowerCase();
            const target = link.originalUrl.toLowerCase();
            return code.includes(filterQuery) || target.includes(filterQuery);
        });

        if (filtered.length > 0) {
            filtered.forEach(link => {
                const tr = document.createElement('tr');
                const expiryText = link.expiresAt ? formatDate(link.expiresAt) : 'Never';
                const truncatedUrl = link.originalUrl.length > 45 ? link.originalUrl.substring(0, 42) + '...' : link.originalUrl;

                tr.innerHTML = `
                    <td><a href="${link.shortUrl}" target="_blank" class="short-link-cell">${link.shortCode}</a></td>
                    <td><span class="url-trunc" title="${link.originalUrl}">${truncatedUrl}</span></td>
                    <td><span class="badge">${link.clickCount}</span></td>
                    <td>${formatDate(link.createdAt)}</td>
                    <td>${expiryText}</td>
                    <td>
                        <button class="btn-danger-sm delete-btn" data-code="${link.shortCode}">
                            <i class="fa-solid fa-trash-can"></i> Delete
                        </button>
                    </td>
                `;

                // Add delete button listener
                tr.querySelector('.delete-btn').addEventListener('click', async (e) => {
                    const code = e.currentTarget.getAttribute('data-code');
                    if (confirm(`Are you sure you want to delete short link '${code}'?`)) {
                        await deleteLink(code);
                    }
                });

                linksTableBody.appendChild(tr);
            });
        } else {
            linksTableBody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 30px;">No matching short links found.</td></tr>';
        }
    }

    // Delete Short Link mapping
    async function deleteLink(shortCode) {
        try {
            const response = await fetch(`/api/urls/${shortCode}`, {
                method: 'DELETE'
            });
            if (response.ok) {
                // If currently showing this result in result active view, hide it
                if (detailAlias.textContent === shortCode) {
                    resultActive.classList.add('hidden');
                    resultEmpty.classList.remove('hidden');
                    resultCard.classList.add('empty-state');
                }
                refreshDashboard();
            } else {
                alert('Failed to delete mapping.');
            }
        } catch (err) {
            console.error('Delete link error: ', err);
        }
    }

    // Render area line chart for hourly analytics
    function renderChart(hourlyClicksData) {
        const ctx = document.getElementById('clickChart').getContext('2d');
        
        // Destruct previous chart instance if exists
        if (clickChart !== null) {
            clickChart.destroy();
        }

        // Construct labels (Hours 0 - 23) and values
        const labels = [];
        const values = [];
        for (let h = 0; h < 24; h++) {
            const hourLabel = h < 10 ? `0${h}:00` : `${h}:00`;
            labels.push(hourLabel);
            values.push(hourlyClicksData[h] || 0);
        }

        // Create canvas color gradient fill
        const gradient = ctx.createLinearGradient(0, 0, 0, 230);
        gradient.addColorStop(0, 'rgba(99, 102, 241, 0.4)');
        gradient.addColorStop(1, 'rgba(99, 102, 241, 0)');

        clickChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Redirection Clicks',
                    data: values,
                    borderColor: '#3b82f6',
                    borderWidth: 3,
                    pointBackgroundColor: '#3b82f6',
                    pointBorderColor: '#fff',
                    pointBorderWidth: 1.5,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    backgroundColor: gradient,
                    fill: true,
                    tension: 0.35
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    x: {
                        grid: { color: 'rgba(255, 255, 255, 0.03)' },
                        ticks: { color: '#64748b', font: { family: 'Plus Jakarta Sans', size: 10 } }
                    },
                    y: {
                        grid: { color: 'rgba(255, 255, 255, 0.03)' },
                        ticks: { color: '#64748b', precision: 0, font: { family: 'Plus Jakarta Sans', size: 10 } }
                    }
                }
            }
        });
    }

    // Helper: format ISO date to readable string
    function formatDate(isoString) {
        if (!isoString) return '';
        const date = new Date(isoString);
        return date.toLocaleDateString(undefined, { 
            month: 'short', 
            day: 'numeric', 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    }
});
