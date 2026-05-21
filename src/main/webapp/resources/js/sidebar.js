// Sidebar Right Toggle Script
function toggleSidebar() {
    document.body.classList.toggle('sidebar-collapsed');
    
    // Store user preference
    const collapsed = document.body.classList.contains('sidebar-collapsed');
    localStorage.setItem('sidebar-collapsed', collapsed);
}

// Mobile sidebar support (overlay backdrop click to close)
document.addEventListener('DOMContentLoaded', function() {
    // Restore collapse state on page load
    const isCollapsed = localStorage.getItem('sidebar-collapsed');
    if (isCollapsed === 'true') {
        document.body.classList.add('sidebar-collapsed');
    } else if (isCollapsed === 'false') {
        document.body.classList.remove('sidebar-collapsed');
    }
    
    // Add backdrop for mobile screens
    if (!document.querySelector('.sidebar-backdrop')) {
        const backdrop = document.createElement('div');
        backdrop.className = 'sidebar-backdrop';
        backdrop.addEventListener('click', function() {
            document.body.classList.add('sidebar-collapsed');
            localStorage.setItem('sidebar-collapsed', 'true');
        });
        document.body.appendChild(backdrop);
    }
});
