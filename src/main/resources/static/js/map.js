document.addEventListener('DOMContentLoaded', function () {
    var map = L.map('map').setView([53.55, 9.99], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    var markers = {};
    var locationMarkers = {};
    var sidebarCollapsed = false;
    var allVehicles = [];
    var csrfToken = null;
    var latLngMap = null;
    var latLngMarker = null;

    // Check if user is admin via header/meta or session
    function isAdmin() {
        return document.querySelector('meta[name="_csrf"]') !== null;
    }

    function getCsrfToken() {
        if (!csrfToken) {
            csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
        }
        return csrfToken;
    }

    var statusLabels = {
        1: 'Frei über Funk',
        2: 'Frei auf Wache',
        3: 'Einsatz übernommen',
        4: 'Am Einsatzort',
        6: 'Außer Dienst'
    };

    var allStatusCodes = [1, 2, 3, 4, 6];

    // Check-in state
    var currentCheckinVehicleId = null;
    var locationWatchId = null;
    var lastPositionSentAt = 0;
    var positionThrottleDelay = 10000; // 10 seconds minimum between updates

    function getMyCheckin() {
        fetch('/api/checkin/me')
            .then(function(response) {
                if (response.status === 204) {
                    currentCheckinVehicleId = null;
                    return;
                }
                return response.json();
            })
            .then(function(data) {
                if (data && data.vehicleId) {
                    currentCheckinVehicleId = data.vehicleId;
                    startLocationWatchForVehicle(currentCheckinVehicleId);
                }
            })
            .catch(function(err) {
                console.error('Failed to get my check-in:', err);
            });
    }

    function startLocationWatchForVehicle(vehicleId) {
        if (!navigator.geolocation) {
            alert('Geolocation wird von Ihrem Browser nicht unterstutzt.');
            return;
        }

        stopLocationWatch();

        var successCallback = function(position) {
            var now = Date.now();
            if (now - lastPositionSentAt >= positionThrottleDelay) {
                updateVehiclePositionFromGeolocation(vehicleId, position);
                lastPositionSentAt = now;
            }
        };

        var errorCallback = function(error) {
            console.error('Geolocation error:', error);
        };

        locationWatchId = navigator.geolocation.watchPosition(successCallback, errorCallback, {
            enableHighAccuracy: true,
            maximumAge: 5000,
            timeout: 10000
        });
    }

    function stopLocationWatch() {
        if (locationWatchId !== null) {
            navigator.geolocation.clearWatch(locationWatchId);
            locationWatchId = null;
        }
    }

    function updateVehiclePositionFromGeolocation(vehicleId, position) {
        var lat = position.coords.latitude;
        var lng = position.coords.longitude;

        if (lat < 53.3 || lat > 53.8 || lng < 9.6 || lng > 10.4) {
            console.warn('Position outside Hamburg bounds:', lat, lng);
            return;
        }

        var token = getCsrfToken();
        var header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        var headers = { 'Content-Type': 'application/json' };
        headers[header] = token;

        fetch('/api/vehicles/' + vehicleId + '/position', {
            method: 'PATCH',
            headers: headers,
            body: JSON.stringify({ lat: lat, lng: lng })
        }).then(function(response) {
            if (!response.ok) throw new Error('Position update failed');
            return response.json();
        }).then(function(updatedVehicle) {
            if (updatedVehicle && markers[vehicleId]) {
                markers[vehicleId].setLatLng([lat, lng]);
            }
        }).catch(function(err) {
            console.error('Position update error:', err);
        });
    }

    function getStatusColor(status) {
        if (status === 1 || status === 2) return '#2ecc71';
        if (status === 3 || status === 4) return '#e74c3c';
        if (status === 6) return '#95a5a6';
        return '#888888';
    }

    function formatUpdatedAt(dateStr) {
        var d = new Date(dateStr);
        var day = String(d.getDate()).padStart(2, '0');
        var month = String(d.getMonth() + 1).padStart(2, '0');
        var year = d.getFullYear();
        var hours = String(d.getHours()).padStart(2, '0');
        var minutes = String(d.getMinutes()).padStart(2, '0');
        return day + '.' + month + '.' + year + ' ' + hours + ':' + minutes;
    }

    function toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        var overlay = document.getElementById('sidebar');
        var btn = document.getElementById('toggleBtn');

        if (sidebarCollapsed) {
            overlay.classList.add('sidebar-collapsed');
            btn.textContent = '▲';
        } else {
            overlay.classList.remove('sidebar-collapsed');
            btn.textContent = '▼';
        }
    }

    function openMobileSidebar() {
        var overlay = document.getElementById('sidebar');
        var fab = document.getElementById('sidebar-fab');
        if (overlay && fab) {
            overlay.classList.add('mobile-open');
            fab.style.display = 'none';
        }
    }

    function closeMobileSidebar() {
        var overlay = document.getElementById('sidebar');
        var fab = document.getElementById('sidebar-fab');
        if (overlay && fab) {
            overlay.classList.remove('mobile-open');
            fab.style.display = 'flex';
        }
    }

    function setupLatLngMap() {
        var mapDiv = document.getElementById('latLngMap');
        if (!mapDiv || latLngMap) return;

        latLngMap = L.map(mapDiv).setView([53.55, 9.99], 12);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(latLngMap);

        latLngMap.on('click', function(e) {
            var latInput = document.getElementById('lat');
            var lngInput = document.getElementById('lng');
            if (latInput && lngInput) {
                latInput.value = e.latlng.lat.toFixed(6);
                lngInput.value = e.latlng.lng.toFixed(6);
            }
            if (latLngMarker) {
                latLngMap.removeLayer(latLngMarker);
            }
            latLngMarker = L.marker(e.latlng).addTo(latLngMap);
        });
    }

    function changeStatus(vehicleId, newStatus) {
        var token = getCsrfToken();
        var header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        var headers = { 'Content-Type': 'application/json' };
        headers[header] = token;

        // Update UI immediately with optimistic update
        if (markers[vehicleId]) {
            markers[vehicleId].setStyle({
                color: getStatusColor(newStatus),
                fillColor: getStatusColor(newStatus)
            });
            var vehicleData = allVehicles.find(function(v) { return v.id == vehicleId; });
            if (vehicleData) vehicleData.status = newStatus;
        }

        fetch('/api/vehicles/' + vehicleId + '/status', {
            method: 'PUT',
            headers: headers,
            body: JSON.stringify({ status: newStatus })
        }).then(function (response) {
            if (!response.ok) {
                console.error('Status change failed with status:', response.status);
                // Revert on error
                var oldStatus = allVehicles.find(function(v) { return v.id == vehicleId; })?.status;
                if (markers[vehicleId] && oldStatus !== undefined) {
                    markers[vehicleId].setStyle({
                        color: getStatusColor(oldStatus),
                        fillColor: getStatusColor(oldStatus)
                    });
                }
                return;
            }
            return response.json();
        }).then(function (updatedVehicle) {
            // Sync with server state
            if (updatedVehicle && allVehicles) {
                var idx = allVehicles.findIndex(function(v) { return v.id == vehicleId; });
                if (idx >= 0) allVehicles[idx] = updatedVehicle;
            }
        }).catch(function (err) {
            console.error('Status change failed:', err);
        }).then(function() {
            // Poll immediately after status change
            fetchData();
        });
    }

    function updateVehiclePosition(vehicleId, lat, lng) {
        var token = getCsrfToken();
        var header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        var headers = { 'Content-Type': 'application/json' };
        headers[header] = token;

        fetch('/api/vehicles/' + vehicleId + '/position', {
            method: 'PATCH',
            headers: headers,
            body: JSON.stringify({ lat: lat, lng: lng })
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('Position update failed');
            }
            return response.json();
        }).then(function (updatedVehicle) {
            if (updatedVehicle && markers[vehicleId]) {
                markers[vehicleId].setLatLng([lat, lng]);
            }
        }).catch(function (err) {
            console.error('Position update error:', err);
        });
    }

    function assignVehicleToLocation(vehicleId, locationId) {
        var token = getCsrfToken();
        var header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        var headers = { 'Content-Type': 'application/json' };
        headers[header] = token;

        fetch('/api/vehicles/' + vehicleId + '/location', {
            method: 'PATCH',
            headers: headers,
            body: JSON.stringify({ locationId: locationId })
        }).then(function (response) {
            if (!response.ok) {
                throw new Error('Location assignment failed');
            }
            return response.json();
        }).catch(function (err) {
            console.error('Location assignment error:', err);
        });
    }

    function createVehiclePopupContent(vehicle) {
        var html = '<b>' + vehicle.callsign + '</b><br/>';
        html += vehicle.type + '<br/>';
        html += '<i>Status:</i> ' + (statusLabels[vehicle.status] || vehicle.status) + '<br/>';
        html += '<i>Letzte Aktualisierung:</i> ' + formatUpdatedAt(vehicle.updatedAt);
        html += '<hr/>';

        var isMyCheckin = currentCheckinVehicleId === vehicle.id;
        if (isMyCheckin) {
            html += '<div style="background:#e8f5e9;padding:6px;margin:4px 0;border-radius:4px;">';
            html += '✓ <b>Sie sind in diesem Fahrzeug eingecheckt</b><br/>';
            html += '<button onclick="window.checkoutVehicle(' + vehicle.id + ')" style="padding:8px 12px;min-height:44px;display:inline-flex;align-items:center;justify-content:center;">Auschecken</button>';
            html += '</div>';
        } else {
            if (currentCheckinVehicleId) {
                html += '<div style="background:#fff3e0;padding:6px;margin:4px 0;border-radius:4px;">';
                html += '⚠ <b>Sie sind in einem anderen Fahrzeug eingecheckt</b>';
                html += '</div>';
            }
            html += '<button onclick="window.checkinVehicle(' + vehicle.id + ')" style="padding:8px 12px;min-height:44px;display:inline-flex;align-items:center;justify-content:center;">Einchecken</button>';
        }

        for (var i = 0; i < allStatusCodes.length; i++) {
            var code = allStatusCodes[i];
            var activeClass = vehicle.status === code ? ' status-active' : '';
            html += '<button class="status-btn' + activeClass + '" onclick="window.changeMarkerStatus(' + vehicle.id + ', ' + code + ');" data-vehicle-id="' + vehicle.id + '" data-status="' + code + '">' + statusLabels[code] + '</button><br/>';
        }

        return html;
    }

    function createLocationPopupContent(location) {
        var html = '<b>' + location.name + '</b><br/>';
        html += '<i>Typ:</i> ' + (location.location_type === 'STATION' ? 'Feuerwache' : 'Einsatzort') + '<br/>';

        if (location.description && location.description.trim() !== '') {
            html += '<hr/><i>Beschreibung:</i><br/>';
            html += location.description;
        }

        if (location.vehicles && location.vehicles.length > 0) {
            html += '<hr/><b>Fahrzeuge:</b><br/>';
            location.vehicles.forEach(function (v) {
                var statusColor = getStatusColor(v.status);
                html += '<div style="margin:4px 0;">';
                html += '<span style="display:inline-block;width:12px;height:12px;background:' + statusColor + ';border-radius:50%;vertical-align:middle;margin-right:6px;"></span>';
                html += v.callsign + ' (' + (statusLabels[v.status] || v.status) + ')';
                html += '</div>';
            });
        } else {
            html += '<br/><i>Keine Fahrzeuge zugewiesen</i>';
        }

        if (isAdmin()) {
            html += '<hr/><div class="assign-vehicle-section">';
            html += '<label>Fahrzeug zuweisen:</label>';
            html += '<select id="assign-vehicle-' + location.id + '" onchange="window.assignSelectedVehicle(' + location.id + ')">';
            html += '<option value="">Bitte wählen...</option>';

            // Filter out vehicles already assigned to this location
            var assignedIds = (location.vehicles || []).map(function(v) { return v.id; });

            allVehicles.forEach(function (v) {
                if (!assignedIds.includes(v.id)) {
                    html += '<option value="' + v.id + '">' + v.callsign + ' (' + (statusLabels[v.status] || v.status) + ')</option>';
                }
            });

            html += '</select></div>';
        }

        return html;
    }

    window.changeMarkerStatus = function(vehicleId, newStatus) {
        changeStatus(vehicleId, newStatus);
    };

    window.assignSelectedVehicle = function(locationId) {
        var select = document.getElementById('assign-vehicle-' + locationId);
        if (!select || !select.value) return;

        var vehicleId = parseInt(select.value);
        assignVehicleToLocation(vehicleId, locationId);

        // Reset to default option
        select.value = '';
    };

    window.checkinVehicle = function(vehicleId) {
        var token = getCsrfToken();
        var header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        var headers = { 'Content-Type': 'application/json' };
        headers[header] = token;

        fetch('/api/vehicles/' + vehicleId + '/checkin', {
            method: 'POST',
            headers: headers,
            body: ''
        }).then(function(response) {
            if (response.ok) {
                currentCheckinVehicleId = vehicleId;
                startLocationWatchForVehicle(vehicleId);
                fetchData();
            } else {
                console.error('Check-in failed:', response.status);
                alert('Einchecken fehlgeschlagen');
            }
        }).catch(function(err) {
            console.error('Check-in error:', err);
            alert('Einchecken fehlgeschlagen: ' + err.message);
        });
    };

    window.checkoutVehicle = function(vehicleId) {
        var token = getCsrfToken();
        var header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
        var headers = { 'Content-Type': 'application/json' };
        headers[header] = token;

        fetch('/api/vehicles/' + vehicleId + '/checkout', {
            method: 'POST',
            headers: headers,
            body: ''
        }).then(function(response) {
            if (response.ok || response.status === 204) {
                currentCheckinVehicleId = null;
                stopLocationWatch();
                fetchData();
            } else {
                console.error('Checkout failed:', response.status);
                alert('Auschecken fehlgeschlagen');
            }
        }).catch(function(err) {
            console.error('Checkout error:', err);
            alert('Auschecken fehlgeschlagen: ' + err.message);
        });
    };

    function updateSidebar(vehicles, stations, incidents) {
        allVehicles = vehicles;

        var vehicleList = document.getElementById('vehicle-list');
        var stationList = document.getElementById('station-list');
        var incidentList = document.getElementById('incident-list');

        // Sort vehicles by callsign
        var sortedVehicles = vehicles ? vehicles.slice().sort(function(a, b) {
            return a.callsign.localeCompare(b.callsign);
        }) : [];

        // Format vehicles
        vehicleList.innerHTML = '';
        if (vehicles && vehicles.length > 0) {
            vehicles.forEach(function (v) {
                var wasAtLocation = v.location && v.location.id;
                var statusColor = getStatusColor(v.status);

                var item = document.createElement('div');
                item.className = 'list-item';
                item.innerHTML = '<div class="vehicle-info">' +
                    '<span style="color:' + statusColor + ';">●</span> ' +
                    '<strong>' + v.callsign + '</strong><br/>' +
                    '<span class="vehicle-status">' + (statusLabels[v.status] || v.status) + '</span>' +
                    (wasAtLocation ? '' : '<span class="underway">unterwegs</span>') +
                '</div>';

                item.onclick = function() {
                    if (markers[v.id]) {
                        markers[v.id].openPopup();
                        map.flyTo(markers[v.id].getLatLng(), 14);
                    }
                    if (window.matchMedia('(max-width: 600px)').matches) {
                        closeMobileSidebar();
                    }
                };
                vehicleList.appendChild(item);
            });
        } else {
            vehicleList.innerHTML = '<div class="list-item" style="text-align:center;color:#666;">Keine Fahrzeuge</div>';
        }

        // Format stations
        stationList.innerHTML = '';
        if (stations && stations.length > 0) {
            var sortedStations = stations.slice().sort(function(a, b) {
                return a.name.localeCompare(b.name);
            });
            sortedStations.forEach(function (l) {
                var item = document.createElement('div');
                item.className = 'list-item';
                item.innerHTML = '<div>' +
                    '<span class="location-type type-station"></span>' +
                    '<strong>' + l.name + '</strong><br/>' +
                    '<small>(' + (l.location_type === 'STATION' ? 'Feuerwache' : 'Einsatzort') + ')' + '</small>' +
                '</div>';

                item.onclick = function() {
                    if (locationMarkers[l.id]) {
                        locationMarkers[l.id].openPopup();
                        map.flyTo(locationMarkers[l.id].getLatLng(), 14);
                    }
                    if (window.matchMedia('(max-width: 600px)').matches) {
                        closeMobileSidebar();
                    }
                };
                stationList.appendChild(item);
            });
        } else {
            stationList.innerHTML = '<div class="list-item" style="text-align:center;color:#666;">Keine Feuerwachen</div>';
        }

        // Format incidents
        incidentList.innerHTML = '';
        if (incidents && incidents.length > 0) {
            var sortedIncidents = incidents.slice().sort(function(a, b) {
                return a.name.localeCompare(b.name);
            });
            sortedIncidents.forEach(function (l) {
                var item = document.createElement('div');
                item.className = 'list-item';
                item.innerHTML = '<div>' +
                    '<span class="location-type type-incident"></span>' +
                    '<strong>' + l.name + '</strong><br/>' +
                    '<small>(' + (l.location_type === 'STATION' ? 'Feuerwache' : 'Einsatzort') + ')' + '</small>' +
                '</div>';

                item.onclick = function() {
                    if (locationMarkers[l.id]) {
                        locationMarkers[l.id].openPopup();
                        map.flyTo(locationMarkers[l.id].getLatLng(), 14);
                    }
                    if (window.matchMedia('(max-width: 600px)').matches) {
                        closeMobileSidebar();
                    }
                };
                incidentList.appendChild(item);
            });
        } else {
            incidentList.innerHTML = '<div class="list-item" style="text-align:center;color:#666;">Keine aktiven Einsatzorte</div>';
        }
    }

    function updateMap(vehicles, stations, incidents) {
        allVehicles = vehicles;

        var currentVehicleIds = {};
        var currentStationIds = {};
        var currentIncidentIds = {};

        // Process vehicles
        vehicles.forEach(function (v) {
            currentVehicleIds[v.id] = v;

            // Determine lat/lng based on location_id
            var lat = v.lat;
            var lng = v.lng;
            if (v.location && v.location.id) {
                lat = v.location.lat;
                lng = v.location.lng;
            }

            if (!markers[v.id]) {
                var isEditable = isAdmin();
                markers[v.id] = L.circleMarker([lat, lng], {
                    radius: 11,
                    color: '#1a1a1a',
                    weight: 2,
                    fillColor: getStatusColor(v.status),
                    fillOpacity: 1.0,
                    draggable: isEditable
                });
                markers[v.id].bindPopup(createVehiclePopupContent(v));

                if (isEditable) {
                    var oldLatLng;
                    markers[v.id].on('dragstart', function() {
                        oldLatLng = this.getLatLng();
                    });
                    markers[v.id].on('dragend', function(e) {
                        var newLatLng = e.target.getLatLng();
                        updateVehiclePosition(v.id, newLatLng.lat, newLatLng.lng)
                            .catch(function(err) {
                                if (oldLatLng) {
                                    e.target.setLatLng(oldLatLng);
                                }
                            });
                    });
                }

                markers[v.id].addTo(map);
            } else {
                var isEditable = isAdmin();
                if (markers[v.id].getLatLng().lat !== lat || markers[v.id].getLatLng().lng !== lng) {
                    markers[v.id].setLatLng([lat, lng]);
                }
                markers[v.id].setStyle({ color: getStatusColor(v.status), fillColor: getStatusColor(v.status) });
                markers[v.id].setPopupContent(createVehiclePopupContent(v));

                // Update draggable state
                if (markers[v.id].options.draggable !== isEditable) {
                    markers[v.id].setOptions({ draggable: isEditable });
                }
            }
        });

        // Remove old vehicle markers
        Object.keys(markers).forEach(function (id) {
            if (!currentVehicleIds[id]) {
                map.removeLayer(markers[id]);
                delete markers[id];
            }
        });

        // Process stations
        if (stations) {
            stations.forEach(function (loc) {
                currentStationIds[loc.id] = loc;

                if (!locationMarkers[loc.id]) {
                    locationMarkers[loc.id] = L.circleMarker([loc.lat, loc.lng], {
                        radius: 15,
                        color: '#1976d2',
                        weight: 3,
                        fillColor: '#4fc3f7',
                        fillOpacity: 0.8
                    });
                    locationMarkers[loc.id].bindPopup(createLocationPopupContent(loc));
                    locationMarkers[loc.id].addTo(map);
                } else {
                    if (locationMarkers[loc.id].getLatLng().lat !== loc.lat || locationMarkers[loc.id].getLatLng().lng !== loc.lng) {
                        locationMarkers[loc.id].setLatLng([loc.lat, loc.lng]);
                    }
                    locationMarkers[loc.id].setStyle({ color: '#1976d2', fillColor: '#4fc3f7' });
                    locationMarkers[loc.id].setPopupContent(createLocationPopupContent(loc));
                }
            });

            // Remove old station markers
            Object.keys(locationMarkers).forEach(function (id) {
                if (!currentStationIds[id] && !currentIncidentIds[id]) {
                    map.removeLayer(locationMarkers[id]);
                    delete locationMarkers[id];
                }
            });
        }

        // Process incidents
        if (incidents) {
            incidents.forEach(function (loc) {
                currentIncidentIds[loc.id] = loc;

                var isStation = false;
                if (!locationMarkers[loc.id]) {
                    locationMarkers[loc.id] = L.circleMarker([loc.lat, loc.lng], {
                        radius: 15,
                        color: '#fbc02d',
                        weight: 3,
                        fillColor: '#fff9c4',
                        fillOpacity: 0.8
                    });
                    locationMarkers[loc.id].bindPopup(createLocationPopupContent(loc));
                    locationMarkers[loc.id].addTo(map);
                } else {
                    if (locationMarkers[loc.id].getLatLng().lat !== loc.lat || locationMarkers[loc.id].getLatLng().lng !== loc.lng) {
                        locationMarkers[loc.id].setLatLng([loc.lat, loc.lng]);
                    }
                    locationMarkers[loc.id].setStyle({ color: '#fbc02d', fillColor: '#fff9c4' });
                    locationMarkers[loc.id].setPopupContent(createLocationPopupContent(loc));
                }
            });

            // Remove old incident markers
            Object.keys(locationMarkers).forEach(function (id) {
                if (!currentStationIds[id] && !currentIncidentIds[id]) {
                    map.removeLayer(locationMarkers[id]);
                    delete locationMarkers[id];
                }
            });
        }

        // Update sidebar
        updateSidebar(vehicles, stations, incidents);
    }

    function fetchData() {
        var vehiclesPromise = fetch('/api/vehicles').then(function (response) { return response.json(); });
        var stationsPromise = fetch('/api/stations').then(function (response) { return response.json(); });
        var incidentsPromise = fetch('/api/incidents?all=false').then(function (response) { return response.json(); });

        Promise.all([vehiclesPromise, stationsPromise, incidentsPromise]).then(function (results) {
            var vehicles = results[0];
            var stations = results[1];
            var incidents = results[2];
            updateMap(vehicles, stations, incidents);
        }).catch(function (err) {
            console.error('Failed to fetch data:', err);
        });
    }

    fetchData();
    setInterval(fetchData, 10000);

    // Get current user's check-in status on page load
    getMyCheckin();

    // Fetch and display app version
    fetch('/api/version')
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            var versionSpan = document.getElementById('app-version');
            if (versionSpan && data.version) {
                versionSpan.textContent = 'v' + data.version;
            }
        })
        .catch(function(err) {
            console.error('Failed to fetch app version:', err);
        });

    // Setup location map if element exists
    setupLatLngMap();
});
