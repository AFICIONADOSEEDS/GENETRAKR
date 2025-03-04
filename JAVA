// Global state
const state = {
  rooms: [{
    id: 'default',
    name: 'Main Room',
    type: 'Flowering Space',
    color: '#dcfce7',
    dimensions: { width: 20, height: 15 },
    growAreas: [],
    plants: []
  }],
  activeRoomId: 'default',
  gridSize: 20, // 20px = 1 foot for display
  gridSnapSize: 1, // 1 foot snap
  selectedArea: null,
  selectedPlant: null,
  isDragging: false,
  dragOffset: { x: 0, y: 0 },
  dragTarget: null, // 'area' or 'plant'
  currentTool: 'select',
  newPlant: {
    name: '',
    strain: '',
    breeder: '',
    category: 'Hybrid',
    sex: 'Unknown',
    stage: 'Seedling',
    color: '#3cb371',
    position: { x: 0, y: 0 },
    areaId: null,
    tags: [],
    breedingTags: [],
    notes: ''
  },
  defaultPlantColors: {
    'Indica': '#6b8e23',
    'Sativa': '#2e8b57',
    'Hybrid': '#3cb371',
    'CBD': '#20b2aa',
    'Auto-flowering': '#228b22'
  },
  roomTypeColors: {
    'Clone Space': '#a7f3d0',
    'Mom Space': '#fde68a',
    'Vegetative Space': '#86efac',
    'Flowering Space': '#c4b5fd',
    'Male/Pollen Collection': '#bfdbfe',
    'Back Up Veg Area': '#e5e7eb',
    'Custom': '#dcfce7'
  },
  activeTab: 'grow-room',
  tagInput: '',
  currentTags: [],
  currentBreedingTags: [],
  showPlantNames: false,
  showDimensions: false,
  breedingTagOptions: [
    'Heavy Resin', 'Great Structure', 'Small Structure', 'Medium Structure',
    'Strong Nose', 'Weak Nose', 'Stem Rub', 'Strong Vigor', 'Weak Vigor'
  ],
  potentialDropArea: null,
  viewAllMode: false,
  viewScale: 1.0,
  dragStartPos: { x: 0, y: 0 }  // Track starting position for drag operations
};

// Generate a unique ID
const generateId = () => `id-${Math.random().toString(36).substr(2, 9)}`;

// Get default color for plant category
const getPlantColor = (category) => {
  return state.defaultPlantColors[category] || '#3cb371';
};

// Get active room
const getActiveRoom = () => {
  return state.rooms.find(room => room.id === state.activeRoomId);
};

// Get room by ID
const getRoomById = (roomId) => {
  return state.rooms.find(room => room.id === roomId);
};

// Update the canvas size based on room dimensions
const updateCanvasSize = () => {
  const activeRoom = getActiveRoom();
  const canvas = document.getElementById('canvas');
  canvas.style.width = `${activeRoom.dimensions.width * state.gridSize}px`;
  canvas.style.height = `${activeRoom.dimensions.height * state.gridSize}px`;
  document.getElementById('roomSize').textContent = `${activeRoom.dimensions.width}' × ${activeRoom.dimensions.height}'`;
  document.getElementById('gridSnap').textContent = `${state.gridSnapSize}'`;
  drawGrid();
};

// Draw grid lines
const drawGrid = () => {
  const canvas = document.getElementById('canvas');
  // Clear existing grid
  canvas.querySelectorAll('.grid-line-horizontal, .grid-line-vertical').forEach(line => line.remove());
  
  const activeRoom = getActiveRoom();
  const { width, height } = activeRoom.dimensions;
  const { gridSize, gridSnapSize } = state;
  
  // Horizontal grid lines
  for (let y = 0; y <= height; y += gridSnapSize) {
    const line = document.createElement('div');
    line.className = 'grid-line-horizontal';
    line.style.top = `${y * gridSize}px`;
    canvas.appendChild(line);
  }
  
  // Vertical grid lines
  for (let x = 0; x <= width; x += gridSnapSize) {
    const line = document.createElement('div');
    line.className = 'grid-line-vertical';
    line.style.left = `${x * gridSize}px`;
    canvas.appendChild(line);
  }
};

// Add a new grow area
const addGrowArea = (width, height, position, color = '#dcfce7', opacity = 0.7) => {
  const { gridSize, gridSnapSize } = state;
  const activeRoom = getActiveRoom();
  
  // Snap to grid
  const snappedPosition = {
    x: Math.round(position.x / (gridSize * gridSnapSize)) * (gridSize * gridSnapSize),
    y: Math.round(position.y / (gridSize * gridSnapSize)) * (gridSize * gridSnapSize)
  };
  
  const widthInGridUnits = width * gridSize;
  const heightInGridUnits = height * gridSize;
  
  const bgColor = `rgba(${parseInt(color.slice(1, 3), 16)}, ${parseInt(color.slice(3, 5), 16)}, ${parseInt(color.slice(5, 7), 16)}, ${opacity})`;
  
  const newArea = {
    id: generateId(),
    width: widthInGridUnits,
    height: heightInGridUnits,
    position: snappedPosition,
    type: `${width}x${height}`,
    color: bgColor,
    borderColor: color
  };
  
  activeRoom.growAreas.push(newArea);
  renderGrowAreas();
  updateCounters();
  
  // Return the new area's ID and automatically select it
  selectArea(newArea.id);
  return newArea.id;
};

// Render grow areas
const renderGrowAreas = () => {
  const canvas = document.getElementById('canvas');
  const activeRoom = getActiveRoom();
  
  // Remove existing areas
  canvas.querySelectorAll('.grow-area').forEach(area => area.remove());
  
  // Add updated areas
  activeRoom.growAreas.forEach(area => {
    const areaEl = document.createElement('div');
    areaEl.className = `grow-area ${state.selectedArea === area.id ? 'selected' : ''}`;
    areaEl.id = `area-${area.id}`;
    areaEl.style.left = `${area.position.x}px`;
    areaEl.style.top = `${area.position.y}px`;
    areaEl.style.width = `${area.width}px`;
    areaEl.style.height = `${area.height}px`;
    areaEl.style.backgroundColor = area.color || 'rgba(220, 252, 231, 0.7)';
    areaEl.style.borderColor = area.borderColor || '#16a34a';
    
    const labelEl = document.createElement('span');
    labelEl.className = 'grow-area-label';
    labelEl.textContent = area.type;
    areaEl.appendChild(labelEl);
    
    // Add dimensions label if enabled
    if (state.showDimensions) {
      const dimensionsEl = document.createElement('div');
      dimensionsEl.className = 'area-dimensions';
      dimensionsEl.textContent = `${area.width/state.gridSize}' × ${area.height/state.gridSize}'`;
      areaEl.appendChild(dimensionsEl);
    }
    
    areaEl.addEventListener('mousedown', (e) => handleAreaMouseDown(e, area.id));
    
    // For better drag and drop between areas
    areaEl.addEventListener('dragover', (e) => {
      e.preventDefault();
      if (state.isDragging && state.dragTarget === 'plant') {
        state.potentialDropArea = area.id;
        areaEl.style.borderStyle = 'dashed';
        areaEl.style.borderColor = '#3b82f6'; // Highlight in blue
        areaEl.style.backgroundColor = 'rgba(219, 234, 254, 0.5)'; // Light blue background
      }
    });
    
    areaEl.addEventListener('dragleave', () => {
      if (state.potentialDropArea === area.id) {
        state.potentialDropArea = null;
        areaEl.style.borderStyle = 'solid';
        areaEl.style.borderColor = area.borderColor || '#16a34a';
        areaEl.style.backgroundColor = area.color || 'rgba(220, 252, 231, 0.7)';
      }
    });
    
    areaEl.addEventListener('drop', (e) => {
      e.preventDefault();
      if (state.isDragging && state.dragTarget === 'plant' && state.selectedPlant) {
        moveSelectedPlantToArea(area.id, e);
        areaEl.style.borderStyle = 'solid';
        areaEl.style.borderColor = area.borderColor || '#16a34a';
        areaEl.style.backgroundColor = area.color || 'rgba(220, 252, 231, 0.7)';
        state.potentialDropArea = null;
      }
    });
    
    canvas.appendChild(areaEl);
    
    // Render plants in this area
    renderPlantsInArea(area);
  });
};

// Direct DOM update for dragging - more efficient than full re-render
const updateAreaPosition = (areaId, x, y) => {
  const areaEl = document.getElementById(`area-${areaId}`);
  if (areaEl) {
    areaEl.style.left = `${x}px`;
    areaEl.style.top = `${y}px`;
  }
};

// Move selected plant to a different area
const moveSelectedPlantToArea = (areaId, event) => {
  if (!state.selectedPlant) return;
  
  const activeRoom = getActiveRoom();
  const plantIndex = activeRoom.plants.findIndex(p => p.id === state.selectedPlant);
  
  if (plantIndex === -1) return;
  
  const plant = activeRoom.plants[plantIndex];
  const oldAreaId = plant.areaId;
  
  if (oldAreaId === areaId) return;
  
  const newArea = activeRoom.growAreas.find(a => a.id === areaId);
  if (!newArea) return;
  
  // Calculate position in the new area
  const areaEl = document.getElementById(`area-${areaId}`);
  const rect = areaEl.getBoundingClientRect();
  
  // Adjust position relative to the drop area
  let newX, newY;
  
  if (event) {
    // If dropped with an event, use the drop position
    newX = event.clientX - rect.left;
    newY = event.clientY - rect.top;
  } else {
    // Otherwise center the plant in the new area
    newX = newArea.width / 2;
    newY = newArea.height / 2;
  }
  
  // Ensure plant stays within area bounds
  newX = Math.max(state.gridSize/4, Math.min(newArea.width - state.gridSize/4, newX));
  newY = Math.max(state.gridSize/4, Math.min(newArea.height - state.gridSize/4, newY));
  
  // Snap to grid
  const snapSize = state.gridSize * 0.5;
  newX = Math.round(newX / snapSize) * snapSize;
  newY = Math.round(newY / snapSize) * snapSize;
  
  // Update the plant
  activeRoom.plants[plantIndex] = {
    ...plant,
    areaId: areaId,
    position: { x: newX, y: newY }
  };
  
  renderGrowAreas();
  updateDetailsPanel();
};

// Render plants in a specific area
const renderPlantsInArea = (area) => {
  const areaEl = document.getElementById(`area-${area.id}`);
  if (!areaEl) return;
  
  const activeRoom = getActiveRoom();
  const areaPlants = activeRoom.plants.filter(p => p.areaId === area.id);
  
  areaPlants.forEach(plant => {
    const plantEl = document.createElement('div');
    plantEl.className = `plant ${state.selectedPlant === plant.id ? 'selected' : ''} ${plant.stage.toLowerCase()} ${plant.sex.toLowerCase()}`;
    plantEl.id = `plant-${plant.id}`;
    plantEl.style.left = `${plant.position.x}px`;
    plantEl.style.top = `${plant.position.y}px`;
    plantEl.style.width = `${state.gridSize/2}px`;
    plantEl.style.height = `${state.gridSize/2}px`;
    plantEl.style.backgroundColor = plant.color || getPlantColor(plant.category);
    plantEl.setAttribute('draggable', 'true');
    plantEl.title = `${plant.name} (${plant.strain}) - ${plant.stage}`;
    
    // Add name label if showPlantNames is enabled
    if (state.showPlantNames) {
      const nameLabel = document.createElement('div');
      nameLabel.className = 'plant-name-label';
      nameLabel.textContent = plant.name;
      plantEl.appendChild(nameLabel);
    }
    
    plantEl.addEventListener('mousedown', (e) => handlePlantMouseDown(e, plant.id));
    plantEl.addEventListener('dragstart', (e) => {
      selectPlant(plant.id);
      state.isDragging = true;
      state.dragTarget = 'plant';
      
      // Create a transparent drag image
      const dragImg = document.createElement('div');
      dragImg.style.opacity = '0';
      document.body.appendChild(dragImg);
      e.dataTransfer.setDragImage(dragImg, 0, 0);
      
      // Store the original area ID
      e.dataTransfer.setData('text/plain', plant.id);
      
      // Set cursor style
      plantEl.style.cursor = 'grabbing';
      document.body.style.cursor = 'grabbing';
    });
    
    plantEl.addEventListener('dragend', () => {
      state.isDragging = false;
      state.dragTarget = null;
      plantEl.style.cursor = 'grab';
      document.body.style.cursor = '';
      
      // Reset any highlighted drop areas
      document.querySelectorAll('.grow-area').forEach(area => {
        const areaObj = activeRoom.growAreas.find(a => a.id === area.id.replace('area-', ''));
        if (areaObj) {
          area.style.borderStyle = 'solid';
          area.style.borderColor = areaObj.borderColor || '#16a34a';
          area.style.backgroundColor = areaObj.color || 'rgba(220, 252, 231, 0.7)';
        }
      });
      
      state.potentialDropArea = null;
    });
    
    areaEl.appendChild(plantEl);
  });
};

// Direct DOM update for plant position - more efficient than full re-render
const updatePlantPosition = (plantId, x, y) => {
  const plantEl = document.getElementById(`plant-${plantId}`);
  if (plantEl) {
    plantEl.style.left = `${x}px`;
    plantEl.style.top = `${y}px`;
  }
};

// Render plant list
const renderPlantList = () => {
  const plantListEl = document.getElementById('plantList');
  const activeRoom = getActiveRoom();
  
  if (activeRoom.plants.length === 0) {
    plantListEl.innerHTML = `
      <div class="plant-list-header">
        <div class="plant-name">Name</div>
        <div class="plant-strain">Strain</div>
        <div class="plant-stage">Stage</div>
      </div>
      <div class="empty-state">
        <i class="fas fa-seedling"></i>
        <div class="empty-state-text">No plants added yet</div>
        <button id="quickAddPlantBtn" class="btn btn-primary">
          <i class="fas fa-plus"></i> Add Plant
        </button>
      </div>
    `;
    
    // Add event listener to the quick add button
    const quickAddBtn = document.getElementById('quickAddPlantBtn');
    if (quickAddBtn) {
      quickAddBtn.addEventListener('click', () => {
        openPlantModal();
      });
    }
    return;
  }
  
  let html = `
    <div class="plant-list-header">
      <div class="plant-name">Name</div>
      <div class="plant-strain">Strain</div>
      <div class="plant-stage">Stage</div>
    </div>
  `;
  
  activeRoom.plants.forEach(plant => {
    html += `
      <div id="plant-list-${plant.id}" class="plant-item ${state.selectedPlant === plant.id ? 'selected' : ''}">
        <div class="plant-color" style="background-color: ${plant.color || getPlantColor(plant.category)}"></div>
        <div class="plant-name">${plant.name}</div>
        <div class="plant-strain">${plant.strain}</div>
        <div class="plant-stage">
          <span class="plant-badge ${plant.stage.toLowerCase()}">${plant.stage}</span>
        </div>
      </div>
    `;
  });
  
  plantListEl.innerHTML = html;
  
  // Add event listeners
  activeRoom.plants.forEach(plant => {
    const el = document.getElementById(`plant-list-${plant.id}`);
    if (el) {
      el.addEventListener('click', () => {
        selectPlant(plant.id);
      });
    }
  });
};

// Update area and plant counters
const updateCounters = () => {
  const activeRoom = getActiveRoom();
  document.getElementById('areaCount').textContent = activeRoom.growAreas.length;
  document.getElementById('plantCount').textContent = activeRoom.plants.length;
  
  // Also update inventory counter if it exists
  const totalInventoryCount = document.getElementById('totalInventoryCount');
  if (totalInventoryCount) {
    const totalPlants = state.rooms.reduce((count, room) => count + room.plants.length, 0);
    totalInventoryCount.textContent = `${totalPlants} Plants`;
  }
};

// Update details panel
const updateDetailsPanel = () => {
  const detailsContentEl = document.getElementById('detailsContent');
  const selectedAreaActionsEl = document.getElementById('selectedAreaActions');
  const selectedPlantActionsEl = document.getElementById('selectedPlantActions');
  const activeRoom = getActiveRoom();
  
  if (state.selectedArea) {
    const area = activeRoom.growAreas.find(a => a.id === state.selectedArea);
    if (!area) {
      clearSelection();
      return;
    }
    
    const areaPlants = activeRoom.plants.filter(p => p.areaId === state.selectedArea);
    
    detailsContentEl.innerHTML = `
      <h4 class="section-title">Selected Growing Area</h4>
      <div class="info-card">
        <div class="info-row">
          <span class="info-label">Size:</span>
          <span class="info-value">${area.type}</span>
        </div>
        <div class="info-row">
          <span class="info-label">Plants:</span>
          <span class="info-value">${areaPlants.length}</span>
        </div>
        <div class="info-row">
          <span class="info-label">Position:</span>
          <span class="info-value">(${Math.round(area.position.x / state.gridSize)}, ${Math.round(area.position.y / state.gridSize)})</span>
        </div>
      </div>
    `;
    
    selectedAreaActionsEl.style.display = 'flex';
    selectedPlantActionsEl.style.display = 'none';
  } else if (state.selectedPlant) {
    const plant = activeRoom.plants.find(p => p.id === state.selectedPlant);
    if (!plant) {
      clearSelection();
      return;
    }
    
    let tagsHTML = '';
    if (plant.tags && plant.tags.length > 0) {
      tagsHTML = `
        <div class="info-row">
          <span class="info-label">Tags:</span>
          <div class="tag-list">
            ${plant.tags.map(tag => `<span class="tag">${tag}</span>`).join('')}
          </div>
        </div>
      `;
    }
    
    let breedingTagsHTML = '';
    if (plant.breedingTags && plant.breedingTags.length > 0) {
      breedingTagsHTML = `
        <div class="info-row">
          <span class="info-label">Breeding Tags:</span>
          <div class="tag-list">
            ${plant.breedingTags.map(tag => `<span class="tag">${tag}</span>`).join('')}
          </div>
        </div>
      `;
    }
    
    let breederHTML = '';
    if (plant.breeder && plant.breeder.trim() !== '') {
      breederHTML = `
        <div class="info-row">
          <span class="info-label">Breeder:</span>
          <span class="info-value">${plant.breeder}</span>
        </div>
      `;
    }
    
    let notesHTML = '';
    if (plant.notes && plant.notes.trim() !== '') {
      notesHTML = `
        <div class="info-row" style="flex-direction: column;">
          <span class="info-label">Notes:</span>
          <p style="margin-top: 4px;">${plant.notes}</p>
        </div>
      `;
    }
    
    detailsContentEl.innerHTML = `
      <h4 class="section-title">Selected Plant</h4>
      <div class="info-card">
        <div class="info-row">
          <span class="info-label">Name:</span>
          <span class="info-value">${plant.name}</span>
        </div>
        <div class="info-row">
          <span class="info-label">Strain:</span>
          <span class="info-value">${plant.strain}</span>
        </div>
        ${breederHTML}
        <div class="info-row">
          <span class="info-label">Category:</span>
          <span class="info-value">${plant.category}</span>
        </div>
        <div class="info-row">
          <span class="info-label">Sex:</span>
          <span class="info-value">${plant.sex}</span>
        </div>
        <div class="info-row">
          <span class="info-label">Stage:</span>
          <span class="info-value">
            <span class="plant-badge ${plant.stage.toLowerCase()}">${plant.stage}</span>
          </span>
        </div>
        <div class="info-row">
          <span class="info-label">Color:</span>
          <span class="info-value">
            <span class="plant-color" style="background-color: ${plant.color || getPlantColor(plant.category)}"></span>
          </span>
        </div>
        ${breedingTagsHTML}
        ${tagsHTML}
        ${notesHTML}
      </div>
    `;
    
    selectedAreaActionsEl.style.display = 'none';
    selectedPlantActionsEl.style.display = 'flex';
  } else {
    detailsContentEl.innerHTML = `
      <p>Select a growing area or plant to view details</p>
      <p>Growing Areas: ${activeRoom.growAreas.length}</p>
      <p>Total Plants: ${activeRoom.plants.length}</p>
    `;
    
    selectedAreaActionsEl.style.display = 'none';
    selectedPlantActionsEl.style.display = 'none';
  }
};

// FIXED: Handle area mouse down - Fixed area dragging issues
const handleAreaMouseDown = (e, areaId) => {
  e.stopPropagation();
  e.preventDefault(); // Prevent any default browser behavior
  
  const activeRoom = getActiveRoom();
  const area = activeRoom.growAreas.find(a => a.id === areaId);
  
  if (!area) return;
  
  if (state.currentTool === 'select') {
    selectArea(area.id);
    
    state.isDragging = true;
    state.dragTarget = 'area';
    
    // Record the exact starting position of the mouse
    state.dragStartPos = {
      x: e.clientX,
      y: e.clientY
    };
    
    // Calculate the offset within the area for more natural dragging
    const rect = e.currentTarget.getBoundingClientRect();
    state.dragOffset = {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top
    };
    
    // Visual feedback for dragging
    e.currentTarget.style.cursor = 'grabbing';
    document.body.style.cursor = 'grabbing';
    
    // Apply enhanced selection styling
    e.currentTarget.style.boxShadow = '0 0 10px rgba(59, 130, 246, 0.5)';
    e.currentTarget.style.zIndex = '100';
  } else if (state.currentTool === 'plant') {
    // Calculate position for new plant
    const rect = e.currentTarget.getBoundingClientRect();
    const relativeX = e.clientX - rect.left;
    const relativeY = e.clientY - rect.top;
    
    // Set new plant details
    state.newPlant = {
      ...state.newPlant,
      position: { 
        x: Math.round(relativeX / (state.gridSize * 0.5)) * (state.gridSize * 0.5), 
        y: Math.round(relativeY / (state.gridSize * 0.5)) * (state.gridSize * 0.5)
      },
      areaId: area.id
    };
    
    openPlantModal();
  } else if (state.currentTool === 'multiplePlants') {
    // Calculate position for new plants
    const rect = e.currentTarget.getBoundingClientRect();
    const relativeX = e.clientX - rect.left;
    const relativeY = e.clientY - rect.top;
    
    // Set new plant details
    state.newPlant = {
      ...state.newPlant,
      position: { 
        x: Math.round(relativeX / (state.gridSize * 0.5)) * (state.gridSize * 0.5), 
        y: Math.round(relativeY / (state.gridSize * 0.5)) * (state.gridSize * 0.5)
      },
      areaId: area.id
    };
    
    openMultiplePlantsModal();
  }
};

// FIXED: Handle plant mouse down - fixed plant dragging
const handlePlantMouseDown = (e, plantId) => {
  if (state.currentTool === 'select') {
    e.stopPropagation();
    e.preventDefault(); // Prevent default browser behaviors
    
    selectPlant(plantId);
    
    state.isDragging = true;
    state.dragTarget = 'plant';
    
    // Record exact client coordinates where the drag started
    state.dragStartPos = {
      x: e.clientX,
      y: e.clientY
    };
    
    // Compute offset within the plant element for more precise dragging
    const rect = e.currentTarget.getBoundingClientRect();
    state.dragOffset = {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top
    };
    
    // Visual feedback for dragging
    e.currentTarget.style.cursor = 'grabbing';
    document.body.style.cursor = 'grabbing';
    
    // Add additional visual feedback
    e.currentTarget.style.boxShadow = '0 0 8px rgba(59, 130, 246, 0.7)';
    e.currentTarget.style.zIndex = '150';
  }
};

// FIXED: Handle canvas click - improved area placement
const handleCanvasClick = (e) => {
  if (state.currentTool === 'area4x4' || state.currentTool === 'area5x5') {
    const rect = document.getElementById('canvas').getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;
    
    const size = state.currentTool === 'area4x4' ? 4 : 5;
    
    // Add the grow area and automatically select it
    addGrowArea(size, size, { x: clickX, y: clickY });
    
    // Switch back to select tool after creating an area
    setTool('select');
  } else if (state.currentTool === 'select') {
    // Only clear selection if we're clicking on an empty part of the canvas
    if (e.target.id === 'canvas') {
      clearSelection();
    }
  }
};

// FIXED: Handle mouse move - better horizontal and vertical dragging
const handleMouseMove = (e) => {
  if (!state.isDragging) return;
  
  e.preventDefault(); // Prevent scrolling during drag
  
  // Calculate the current position of the mouse
  const mouseX = e.clientX;
  const mouseY = e.clientY;
  
  // Calculate how far the mouse has moved from its starting position
  const deltaX = mouseX - state.dragStartPos.x;
  const deltaY = mouseY - state.dragStartPos.y;
  
  const activeRoom = getActiveRoom();
  
  if (state.selectedArea !== null && state.dragTarget === 'area') {
    const areaIndex = activeRoom.growAreas.findIndex(a => a.id === state.selectedArea);
    if (areaIndex !== -1) {
      const area = activeRoom.growAreas[areaIndex];
      
      // Get the original position
      const origPos = { ...area.position };
      
      // Calculate new position with the delta movement
      let newX = origPos.x + deltaX;
      let newY = origPos.y + deltaY;
      
      // Apply grid snapping
      if (state.gridSnapSize > 0) {
        const snapSize = state.gridSize * state.gridSnapSize;
        newX = Math.round(newX / snapSize) * snapSize;
        newY = Math.round(newY / snapSize) * snapSize;
      }
      
      // Ensure area stays within canvas bounds
      const boundedX = Math.max(0, Math.min(activeRoom.dimensions.width * state.gridSize - area.width, newX));
      const boundedY = Math.max(0, Math.min(activeRoom.dimensions.height * state.gridSize - area.height, newY));
      
      // Update the DOM directly for immediate visual feedback
      updateAreaPosition(area.id, boundedX, boundedY);
      
      // Update the area position in state
      activeRoom.growAreas[areaIndex] = {
        ...area,
        position: { x: boundedX, y: boundedY }
      };
      
      // Update the starting position for the next movement calculation
      state.dragStartPos = {
        x: mouseX,
        y: mouseY
      };
    }
  } else if (state.selectedPlant !== null && state.dragTarget === 'plant') {
    const plantIndex = activeRoom.plants.findIndex(p => p.id === state.selectedPlant);
    if (plantIndex !== -1) {
      const plant = activeRoom.plants[plantIndex];
      const area = activeRoom.growAreas.find(a => a.id === plant.areaId);
      
      if (area) {
        const areaEl = document.getElementById(`area-${area.id}`);
        const areaRect = areaEl.getBoundingClientRect();
        
        // Calculate position relative to the area
        const relativeX = mouseX - areaRect.left - state.dragOffset.x;
        const relativeY = mouseY - areaRect.top - state.dragOffset.y;
        
        // Ensure plant stays within area bounds
        const newX = Math.max(0, Math.min(area.width - state.gridSize/2, relativeX));
        const newY = Math.max(0, Math.min(area.height - state.gridSize/2, relativeY));
        
        // Apply snap grid to the plant position
        const snapSize = state.gridSize * 0.5;
        const snappedX = Math.round(newX / snapSize) * snapSize;
        const snappedY = Math.round(newY / snapSize) * snapSize;
        
        // Update the DOM directly for immediate visual feedback
        updatePlantPosition(plant.id, snappedX, snappedY);
        
        // Update plant position in state
        activeRoom.plants[plantIndex] = {
          ...plant,
          position: { x: snappedX, y: snappedY }
        };
        
        // Update the starting position for the next movement calculation
        state.dragStartPos = {
          x: mouseX,
          y: mouseY
        };
      }
    }
  }
};

// Set current tool
const setTool = (tool) => {
  state.currentTool = tool;
  
  // Update tool buttons
  document.getElementById('selectTool').className = `btn ${tool === 'select' ? 'btn-active' : 'btn-default'}`;
  document.getElementById('area4x4Tool').className = `btn ${tool === 'area4x4' ? 'btn-active' : 'btn-default'}`;
  document.getElementById('area5x5Tool').className = `btn ${tool === 'area5x5' ? 'btn-active' : 'btn-default'}`;
  document.getElementById('customAreaTool').className = `btn ${tool === 'customArea' ? 'btn-active' : 'btn-default'}`;
  document.getElementById('plantTool').className = `btn ${tool === 'plant' ? 'btn-active' : 'btn-default'}`;
  document.getElementById('multiplePlantsTool').className = `btn ${tool === 'multiplePlants' ? 'btn-active' : 'btn-default'}`;
  
  // Update cursor style based on the selected tool
  const canvas = document.getElementById('canvas');
  if (tool === 'select') {
    canvas.style.cursor = 'default';
  } else if (tool === 'area4x4' || tool === 'area5x5' || tool === 'customArea') {
    canvas.style.cursor = 'crosshair';
  } else if (tool === 'plant' || tool === 'multiplePlants') {
    canvas.style.cursor = 'cell';
  }
  
  // Show names and dimensions buttons have separate toggle state
  updateToggleButtons();
};

// Update toggle buttons state
const updateToggleButtons = () => {
  document.getElementById('showNamesBtn').className = `btn ${state.showPlantNames ? 'btn-active' : 'btn-default'}`;
  document.getElementById('showDimensionsBtn').className = `btn ${state.showDimensions ? 'btn-active' : 'btn-default'}`;
};

// Toggle show plant names
const toggleShowNames = () => {
  state.showPlantNames = !state.showPlantNames;
  updateToggleButtons();
  renderGrowAreas();
};

// Toggle show dimensions
const toggleShowDimensions = () => {
  state.showDimensions = !state.showDimensions;
  updateToggleButtons();
  renderGrowAreas();
};

// IMPROVED: Select area with enhanced visual feedback
const selectArea = (areaId) => {
  state.selectedArea = areaId;
  state.selectedPlant = null;
  renderGrowAreas();
  renderPlantList();
  updateDetailsPanel();
  document.getElementById('deleteBtn').disabled = false;
  
  // Apply enhanced visual feedback to the selected area
  const areaEl = document.getElementById(`area-${areaId}`);
  if (areaEl) {
    // Add the blue highlight outline effect
    areaEl.classList.add('selected');
    // Make sure it's visible
    areaEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }
};

// Select plant
const selectPlant = (plantId) => {
  state.selectedPlant = plantId;
  state.selectedArea = null;
  renderGrowAreas();
  renderPlantList();
  updateDetailsPanel();
  document.getElementById('deleteBtn').disabled = false;
  
  // Make sure selected plant is visible
  const plantEl = document.getElementById(`plant-${plantId}`);
  if (plantEl) {
    plantEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }
};

// Clear selection
const clearSelection = () => {
  state.selectedArea = null;
  state.selectedPlant = null;
  renderGrowAreas();
  renderPlantList();
  updateDetailsPanel();
  document.getElementById('deleteBtn').disabled = true;
};

// Delete selected item
const deleteSelected = () => {
  const activeRoom = getActiveRoom();
  
  if (state.selectedArea) {
    // Remove area and its plants
    activeRoom.growAreas = activeRoom.growAreas.filter(a => a.id !== state.selectedArea);
    activeRoom.plants = activeRoom.plants.filter(p => p.areaId !== state.selectedArea);
    state.selectedArea = null;
  } else if (state.selectedPlant) {
    activeRoom.plants = activeRoom.plants.filter(p => p.id !== state.selectedPlant);
    state.selectedPlant = null;
  }
  
  renderGrowAreas();
  renderPlantList();
  updateDetailsPanel();
  updateCounters();
  document.getElementById('deleteBtn').disabled = true;
};

// FIXED: Open plant modal with proper initialization
const openPlantModal = () => {
  document.getElementById('plantFormModal').style.display = 'flex';
  document.getElementById('plantName').value = '';
  document.getElementById('plantStrain').value = '';
  document.getElementById('plantBreeder').value = '';
  document.getElementById('plantCategory').value = 'Hybrid';
  document.getElementById('plantSex').value = 'Unknown';
  document.getElementById('plantStage').value = 'Seedling';
  document.getElementById('plantColor').value = getPlantColor('Hybrid');
  document.getElementById('plantColorPreview').style.backgroundColor = getPlantColor('Hybrid');
  document.getElementById('plantNotes').value = '';
  document.getElementById('plantQuantity').value = '1';
  
  // Reset tags
  state.currentTags = [];
  state.currentBreedingTags = [];
  renderTags();
  renderBreedingTags();
  
  // Set focus to the name field
  setTimeout(() => {
    document.getElementById('plantName').focus();
  }, 100);
  
  // Change modal title
  document.querySelector('#plantFormModal .modal-title').textContent = 'Add New Plant';
  document.getElementById('addPlantBtn').textContent = 'Add Plant';
  
  // Make sure the add button has the correct handler
  const addBtn = document.getElementById('addPlantBtn');
  addBtn.onclick = addPlant;
};

// Open multiple plants modal
const openMultiplePlantsModal = () => {
  document.getElementById('multiplePlantsModal').style.display = 'flex';
  document.getElementById('multiplePlantName').value = '';
  document.getElementById('multiplePlantStrain').value = '';
  document.getElementById('multiplePlantBreeder').value = '';
  document.getElementById('multiplePlantCategory').value = 'Hybrid';
  document.getElementById('multiplePlantSex').value = 'Unknown';
  document.getElementById('multiplePlantStage').value = 'Seedling';
  document.getElementById('multiplePlantColor').value = getPlantColor('Hybrid');
  document.getElementById('multiplePlantColorPreview').style.backgroundColor = getPlantColor('Hybrid');
  document.getElementById('multiplePlantQuantity').value = '5';
  
  // Set focus to the name field
  setTimeout(() => {
    document.getElementById('multiplePlantName').focus();
  }, 100);
};

// Render breeding tags
const renderBreedingTags = () => {
  const container = document.getElementById('breedingTagsContainer');
  if (!container) return;
  
  let html = '';
  state.breedingTagOptions.forEach(tag => {
    const isChecked = state.currentBreedingTags.includes(tag) ? 'checked' : '';
    html += `
      <div class="breeding-tag-item">
        <input type="checkbox" id="breeding-tag-${tag.replace(/\s+/g, '-').toLowerCase()}" 
               class="breeding-tag-checkbox" data-tag="${tag}" ${isChecked}>
        <label for="breeding-tag-${tag.replace(/\s+/g, '-').toLowerCase()}">${tag}</label>
      </div>
    `;
  });
  
  container.innerHTML = html;
  
  // Add event listeners
  container.querySelectorAll('.breeding-tag-checkbox').forEach(checkbox => {
    checkbox.addEventListener('change', (e) => {
      const tag = e.target.getAttribute('data-tag');
      if (e.target.checked) {
        if (!state.currentBreedingTags.includes(tag)) {
          state.currentBreedingTags.push(tag);
        }
      } else {
        state.currentBreedingTags = state.currentBreedingTags.filter(t => t !== tag);
      }
    });
  });
};

// Render tags
const renderTags = () => {
  const tagsContainer = document.getElementById('tagsContainer');
  if (!tagsContainer) return;
  
  const tagInput = document.getElementById('plantTags');
  
  // Remove all tags (except the input)
  Array.from(tagsContainer.children).forEach(child => {
    if (child !== tagInput) {
      child.remove();
    }
  });
  
  // Add tags before the input
  state.currentTags.forEach(tag => {
    const tagEl = document.createElement('div');
    tagEl.className = 'tag';
    tagEl.innerHTML = `
      ${tag}
      <span class="remove" data-tag="${tag}">×</span>
    `;
    tagsContainer.insertBefore(tagEl, tagInput);
    
    // Add event listener to remove button
    tagEl.querySelector('.remove').addEventListener('click', (e) => {
      const tagToRemove = e.target.getAttribute('data-tag');
      state.currentTags = state.currentTags.filter(t => t !== tagToRemove);
      renderTags();
    });
  });
};

// FIXED: Add plant with proper event handling
const addPlant = () => {
  const name = document.getElementById('plantName').value.trim();
  const strain = document.getElementById('plantStrain').value.trim();
  const breeder = document.getElementById('plantBreeder').value.trim();
  const quantity = parseInt(document.getElementById('plantQuantity').value) || 1;
  
  if (!name) return;
  
  const activeRoom = getActiveRoom();
  
  // Create the base plant object
  const basePlant = {
    name,
    strain: strain || 'Unknown',
    breeder,
    category: document.getElementById('plantCategory').value,
    sex: document.getElementById('plantSex').value,
    stage: document.getElementById('plantStage').value,
    color: document.getElementById('plantColor').value,
    position: state.newPlant.position,
    areaId: state.newPlant.areaId,
    tags: [...state.currentTags],
    breedingTags: [...state.currentBreedingTags],
    notes: document.getElementById('plantNotes').value
  };
  
  let lastPlantId = null;
  
  // Add plants based on quantity
  for (let i = 0; i < quantity; i++) {
    const plant = {
      ...basePlant,
      id: generateId(),
      name: quantity > 1 ? `${name} #${i + 1}` : name
    };
    
    // Adjust position for each plant if adding multiple
    if (i > 0) {
      const offsetX = (i % 4) * state.gridSize * 0.75;
      const offsetY = Math.floor(i / 4) * state.gridSize * 0.75;
      plant.position = {
        x: Math.min(plant.position.x + offsetX, state.gridSize * 20 - state.gridSize),
        y: Math.min(plant.position.y + offsetY, state.gridSize * 15 - state.gridSize)
      };
    }
    
    activeRoom.plants.push(plant);
    lastPlantId = plant.id;
  }
  
  document.getElementById('plantFormModal').style.display = 'none';
  
  // Reset current tags
  state.currentTags = [];
  state.currentBreedingTags = [];
  
  // Select the last plant we added
  if (lastPlantId) {
    selectPlant(lastPlantId);
  }
  
  renderGrowAreas();
  renderPlantList();
  updateCounters();
};

// Add multiple plants
const addMultiplePlants = () => {
  const name = document.getElementById('multiplePlantName').value.trim();
  const strain = document.getElementById('multiplePlantStrain').value.trim();
  const breeder = document.getElementById('multiplePlantBreeder').value.trim();
  const quantity = parseInt(document.getElementById('multiplePlantQuantity').value) || 5;
  
  if (!name) return;
  
  const activeRoom = getActiveRoom();
  
  // Create the base plant object
  const basePlant = {
    name,
    strain: strain || 'Unknown',
    breeder,
    category: document.getElementById('multiplePlantCategory').value,
    sex: document.getElementById('multiplePlantSex').value,
    stage: document.getElementById('multiplePlantStage').value,
    color: document.getElementById('multiplePlantColor').value,
    position: state.newPlant.position,
    areaId: state.newPlant.areaId,
    tags: [],
    breedingTags: [],
    notes: ''
  };
  
  // Calculate grid layout for multiple plants
  const area = activeRoom.growAreas.find(a => a.id === state.newPlant.areaId);
  if (!area) return;
  
  const areaWidth = area.width;
  const areaHeight = area.height;
  const plantWidth = state.gridSize * 0.75;
  const plantHeight = state.gridSize * 0.75;
  
  // Calculate number of plants per row
  const plantsPerRow = Math.floor(areaWidth / plantWidth);
  if (plantsPerRow === 0) return;
  
  let lastPlantId = null;
  
  // Add plants based on quantity
  for (let i = 0; i < quantity; i++) {
    const plant = {
      ...basePlant,
      id: generateId(),
      name: `${name} #${i + 1}`
    };
    
    const row = Math.floor(i / plantsPerRow);
    const col = i % plantsPerRow;
    
    plant.position = {
      x: Math.min(col * plantWidth + state.gridSize/2, areaWidth - plantWidth),
      y: Math.min(row * plantHeight + state.gridSize/2, areaHeight - plantHeight)
    };
    
    activeRoom.plants.push(plant);
    lastPlantId = plant.id;
  }
  
  document.getElementById('multiplePlantsModal').style.display = 'none';
  
  // Select the last plant we added
  if (lastPlantId) {
    selectPlant(lastPlantId);
  }
  
  renderGrowAreas();
  renderPlantList();
  updateCounters();
};

// Show color change modal
const showColorChangeModal = () => {
  if (!state.selectedPlant) return;
  
  const activeRoom = getActiveRoom();
  const plant = activeRoom.plants.find(p => p.id === state.selectedPlant);
  if (plant) {
    document.getElementById('newPlantColor').value = plant.color || getPlantColor(plant.category);
    document.getElementById('newColorPreview').style.backgroundColor = plant.color || getPlantColor(plant.category);
    document.getElementById('colorChangeModal').style.display = 'flex';
  }
};

// Show area color change modal
const showAreaColorChangeModal = () => {
  if (!state.selectedArea) return;
  
  const activeRoom = getActiveRoom();
  const area = activeRoom.growAreas.find(a => a.id === state.selectedArea);
  if (area) {
    // Extract color from rgba if needed
    let color = area.borderColor || '#dcfce7';
    let opacity = 0.7;
    
    if (area.color && area.color.startsWith('rgba')) {
      // Parse the rgba color to get the hex and opacity
      const rgbaMatch = area.color.match(/rgba\((\d+),\s*(\d+),\s*(\d+),\s*([\d.]+)\)/);
      if (rgbaMatch) {
        const r = parseInt(rgbaMatch[1]);
        const g = parseInt(rgbaMatch[2]);
        const b = parseInt(rgbaMatch[3]);
        opacity = parseFloat(rgbaMatch[4]);
        
        // Convert RGB to HEX
        color = `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
      }
    }
    
    document.getElementById('newAreaColor').value = color;
    document.getElementById('newAreaColorPreview').style.backgroundColor = color;
    document.getElementById('areaColorOpacity').value = opacity;
    document.getElementById('areaColorChangeModal').style.display = 'flex';
  }
};

// Apply color change
const applyColorChange = () => {
  if (!state.selectedPlant) return;
  
  const activeRoom = getActiveRoom();
  const plantIndex = activeRoom.plants.findIndex(p => p.id === state.selectedPlant);
  if (plantIndex !== -1) {
    activeRoom.plants[plantIndex] = {
      ...activeRoom.plants[plantIndex],
      color: document.getElementById('newPlantColor').value
    };
    
    document.getElementById('colorChangeModal').style.display = 'none';
    renderGrowAreas();
    renderPlantList();
    updateDetailsPanel();
  }
};

// Apply area color change
const applyAreaColorChange = () => {
  if (!state.selectedArea) return;
  
  const activeRoom = getActiveRoom();
  const areaIndex = activeRoom.growAreas.findIndex(a => a.id === state.selectedArea);
  if (areaIndex !== -1) {
    const color = document.getElementById('newAreaColor').value;
    const opacity = document.getElementById('areaColorOpacity').value;
    
    // Convert hex color to rgba
    const r = parseInt(color.slice(1, 3), 16);
    const g = parseInt(color.slice(3, 5), 16);
    const b = parseInt(color.slice(5, 7), 16);
    const bgColor = `rgba(${r}, ${g}, ${b}, ${opacity})`;
    
    activeRoom.growAreas[areaIndex] = {
      ...activeRoom.growAreas[areaIndex],
      color: bgColor,
      borderColor: color
    };
    
    document.getElementById('areaColorChangeModal').style.display = 'none';
    renderGrowAreas();
    updateDetailsPanel();
  }
};

// FIXED: Apply room settings with proper validation
const applyRoomSettings = () => {
  const width = parseInt(document.getElementById('roomWidth').value);
  const height = parseInt(document.getElementById('roomHeight').value);
  const gridSnapSize = parseFloat(document.getElementById('gridSnapSize').value);
  
  if (width < 5 || width > 50 || height < 5 || height > 50) {
    alert('Room dimensions must be between 5 and 50 feet.');
    return;
  }
  
  const activeRoom = getActiveRoom();
  activeRoom.dimensions = { width, height };
  state.gridSnapSize = gridSnapSize;
  
  updateCanvasSize();
  document.getElementById('roomSettingsModal').style.display = 'none';
};

// Create custom area
const createCustomArea = () => {
  const width = parseInt(document.getElementById('customAreaWidth').value);
  const height = parseInt(document.getElementById('customAreaHeight').value);
  const color = document.getElementById('customAreaColor').value;
  const opacity = document.getElementById('customAreaColorOpacity').value;
  
  if (width < 1 || width > 20 || height < 1 || height > 20) {
    alert('Area dimensions must be between 1 and 20 feet.');
    return;
  }
  
  state.currentTool = 'customArea';
  document.getElementById('customAreaModal').style.display = 'none';
  
  // Use the center of the canvas as the default position
  const canvasRect = document.getElementById('canvas').getBoundingClientRect();
  const position = {
    x: canvasRect.width / 2 - (width * state.gridSize / 2),
    y: canvasRect.height / 2 - (height * state.gridSize / 2)
  };
  
  addGrowArea(width, height, position, color, opacity);
  setTool('select');
};

// Show resize area modal
const showResizeAreaModal = () => {
  if (!state.selectedArea) return;
  
  const activeRoom = getActiveRoom();
  const area = activeRoom.growAreas.find(a => a.id === state.selectedArea);
  if (area) {
    document.getElementById('resizeAreaWidth').value = area.width / state.gridSize;
    document.getElementById('resizeAreaHeight').value = area.height / state.gridSize;
    document.getElementById('resizeAreaModal').style.display = 'flex';
  }
};

// Apply resize area
const applyResizeArea = () => {
  if (!state.selectedArea) return;
  
  const width = parseInt(document.getElementById('resizeAreaWidth').value);
  const height = parseInt(document.getElementById('resizeAreaHeight').value);
  
  if (width < 1 || width > 20 || height < 1 || height > 20) {
    alert('Area dimensions must be between 1 and 20 feet.');
    return;
  }
  
  const activeRoom = getActiveRoom();
  const areaIndex = activeRoom.growAreas.findIndex(a => a.id === state.selectedArea);
  if (areaIndex !== -1) {
    activeRoom.growAreas[areaIndex] = {
      ...activeRoom.growAreas[areaIndex],
      width: width * state.gridSize,
      height: height * state.gridSize,
      type: `${width}x${height}`
    };
    
    document.getElementById('resizeAreaModal').style.display = 'none';
    renderGrowAreas();
    updateDetailsPanel();
  }
};

// Show add room modal
const showAddRoomModal = () => {
  document.getElementById('roomName').value = '';
  document.getElementById('roomType').value = 'Vegetative Space';
  document.getElementById('newRoomWidth').value = '20';
  document.getElementById('newRoomHeight').value = '15';
  
  // Set color based on room type
  const color = state.roomTypeColors['Vegetative Space'];
  document.getElementById('roomColor').value = color;
  document.getElementById('roomColorPreview').style.backgroundColor = color;
  
  document.getElementById('addRoomModal').style.display = 'flex';
};

// Handle room type change
const handleRoomTypeChange = () => {
  const roomType = document.getElementById('roomType').value;
  const color = state.roomTypeColors[roomType] || '#dcfce7';
  document.getElementById('roomColor').value = color;
  document.getElementById('roomColorPreview').style.backgroundColor = color;
};

// Create new room
const createRoom = () => {
  let name = document.getElementById('roomName').value.trim();
  const type = document.getElementById('roomType').value;
  const width = parseInt(document.getElementById('newRoomWidth').value);
  const height = parseInt(document.getElementById('newRoomHeight').value);
  const color = document.getElementById('roomColor').value;
  
  if (width < 5 || width > 50 || height < 5 || height > 50) {
    alert('Room dimensions must be between 5 and 50 feet.');
    return;
  }
  
  // If no name provided, use the type
  if (!name) {
    name = type;
  }
  
  const newRoom = {
    id: generateId(),
    name,
    type,
    color,
    dimensions: { width, height },
    growAreas: [],
    plants: []
  };
  
  state.rooms.push(newRoom);
  state.activeRoomId = newRoom.id;
  
  document.getElementById('addRoomModal').style.display = 'none';
  renderRoomTabs();
  updateCanvasSize();
  renderGrowAreas();
  renderPlantList();
  updateCounters();
  updateDetailsPanel();
};

// Render room tabs
const renderRoomTabs = () => {
  const tabsContainer = document.getElementById('roomTabs');
  
  if (!tabsContainer) return;
  
  let html = '';
  
  state.rooms.forEach(room => {
    const activeClass = room.id === state.activeRoomId ? 'active' : '';
    const roomTypeIcon = getRoomTypeIcon(room.type);
    
    html += `
      <div id="room-tab-${room.id}" class="room-tab ${activeClass}" data-room-id="${room.id}">
        <i class="${roomTypeIcon}"></i> ${room.name}
        ${state.rooms.length > 1 ? `<span class="room-tab-close" data-room-id="${room.id}"><i class="fas fa-times"></i></span>` : ''}
      </div>
    `;
  });
  
  tabsContainer.innerHTML = html;
  
  // Add event listeners
  state.rooms.forEach(room => {
    const tabEl = document.getElementById(`room-tab-${room.id}`);
    if (tabEl) {
      tabEl.addEventListener('click', (e) => {
        if (!e.target.classList.contains('room-tab-close') && !e.target.closest('.room-tab-close')) {
          switchRoom(room.id);
        }
      });
      
      const closeBtn = tabEl.querySelector('.room-tab-close');
      if (closeBtn) {
        closeBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          const roomId = e.target.closest('.room-tab-close').getAttribute('data-room-id');
          removeRoom(roomId);
        });
      }
    }
  });
};

// Get room type icon
const getRoomTypeIcon = (type) => {
  switch (type) {
    case 'Clone Space': return 'fas fa-baby';
    case 'Mom Space': return 'fas fa-female';
    case 'Vegetative Space': return 'fas fa-seedling';
    case 'Flowering Space': return 'fas fa-cannabis';
    case 'Male/Pollen Collection': return 'fas fa-male';
    case 'Back Up Veg Area': return 'fas fa-shield-alt';
    default: return 'fas fa-leaf';
  }
};

// Switch to a different room
const switchRoom = (roomId) => {
  // Exit View All mode if active
  if (state.viewAllMode) {
    exitViewAllRooms();
  }
  
  state.activeRoomId = roomId;
  clearSelection();
  renderRoomTabs();
  updateCanvasSize();
  renderGrowAreas();
  renderPlantList();
  updateCounters();
  updateDetailsPanel();
};

// Remove room
const removeRoom = (roomId) => {
  if (state.rooms.length <= 1) {
    alert('Cannot remove the last room.');
    return;
  }
  
  if (confirm('Are you sure you want to remove this room and all its contents?')) {
    state.rooms = state.rooms.filter(room => room.id !== roomId);
    
    // If we're removing the active room, switch to the first available
    if (state.activeRoomId === roomId) {
      state.activeRoomId = state.rooms[0].id;
    }
    
    renderRoomTabs();
    updateCanvasSize();
    renderGrowAreas();
    renderPlantList();
    updateCounters();
    updateDetailsPanel();
  }
};

// View All Rooms
const viewAllRooms = () => {
  // Enable view all mode
  state.viewAllMode = true;
  
  // Show view all controls
  document.getElementById('viewAllControls').style.display = 'flex';
  
  // Hide the regular canvas and show the all rooms container
  document.getElementById('canvas').style.display = 'none';
  const allRoomsContainer = document.getElementById('allRoomsContainer');
  allRoomsContainer.style.display = 'grid';
  allRoomsContainer.innerHTML = ''; // Clear previous content
  
  // Add canvas container class for scrolling
  document.querySelector('.canvas-container').classList.add('view-all-mode');
  
  // Calculate the scale for each room preview
  state.viewScale = 0.25; // Default scale
  
  // Render all room previews
  state.rooms.forEach(room => {
    renderRoomPreview(room);
  });
  
  // Fit to screen initially
  fitRoomsToScreen();
};

// Render a single room preview
const renderRoomPreview = (room) => {
  const allRoomsContainer = document.getElementById('allRoomsContainer');
  
  // Create room preview container
  const previewContainer = document.createElement('div');
  previewContainer.className = `room-preview ${room.id === state.activeRoomId ? 'active' : ''}`;
  previewContainer.id = `room-preview-${room.id}`;
  previewContainer.setAttribute('data-room-id', room.id);
  previewContainer.style.backgroundColor = '#f3f4f6';
  
  // Add room title
  const titleEl = document.createElement('div');
  titleEl.className = 'room-preview-title';
  titleEl.innerHTML = `
    <span>${room.name}</span>
    <span class="room-preview-type">${room.type}</span>
  `;
  previewContainer.appendChild(titleEl);
  
  // Add room stats
  const statsEl = document.createElement('div');
  statsEl.className = 'room-preview-stats';
  statsEl.innerHTML = `
    <span>${room.dimensions.width}' × ${room.dimensions.height}'</span>
    <span>${room.plants.length} Plants</span>
  `;
  previewContainer.appendChild(statsEl);
  
  // Create scaled room canvas
  const roomCanvas = document.createElement('div');
  roomCanvas.className = 'room-canvas';
  roomCanvas.style.width = `${room.dimensions.width * state.gridSize * state.viewScale}px`;
  roomCanvas.style.height = `${room.dimensions.height * state.gridSize * state.viewScale}px`;
  roomCanvas.style.transform = `scale(${state.viewScale})`;
  previewContainer.appendChild(roomCanvas);
  
  // Add click handler to switch to the room
  previewContainer.addEventListener('click', () => {
    switchRoom(room.id);
  });
  
  // Add grow areas and plants
  renderRoomPreviewContent(room, roomCanvas);
  
  // Add to container
  allRoomsContainer.appendChild(previewContainer);
};

// Render the content of a room preview (grow areas and plants)
const renderRoomPreviewContent = (room, canvas) => {
  // Add grow areas
  room.growAreas.forEach(area => {
    const areaEl = document.createElement('div');
    areaEl.className = 'grow-area';
    areaEl.style.left = `${area.position.x}px`;
    areaEl.style.top = `${area.position.y}px`;
    areaEl.style.width = `${area.width}px`;
    areaEl.style.height = `${area.height}px`;
    areaEl.style.backgroundColor = area.color || 'rgba(220, 252, 231, 0.7)';
    areaEl.style.borderColor = area.borderColor || '#16a34a';
    
    // Add grow area label
    const labelEl = document.createElement('span');
    labelEl.className = 'grow-area-label';
    labelEl.textContent = area.type;
    areaEl.appendChild(labelEl);
    
    canvas.appendChild(areaEl);
    
    // Add plants in this area
    room.plants.filter(p => p.areaId === area.id).forEach(plant => {
      const plantEl = document.createElement('div');
      plantEl.className = `plant ${plant.stage.toLowerCase()} ${plant.sex.toLowerCase()}`;
      plantEl.style.left = `${plant.position.x}px`;
      plantEl.style.top = `${plant.position.y}px`;
      plantEl.style.width = `${state.gridSize/2}px`;
      plantEl.style.height = `${state.gridSize/2}px`;
      plantEl.style.backgroundColor = plant.color || getPlantColor(plant.category);
      
      areaEl.appendChild(plantEl);
    });
  });
};

// Fit rooms to screen
const fitRoomsToScreen = () => {
  const allRoomsContainer = document.getElementById('allRoomsContainer');
  const previewEls = allRoomsContainer.querySelectorAll('.room-preview');
  
  if (previewEls.length === 0) return;
  
  // Find the maximum room dimensions
  let maxWidth = 0;
  let maxHeight = 0;
  
  state.rooms.forEach(room => {
    maxWidth = Math.max(maxWidth, room.dimensions.width);
    maxHeight = Math.max(maxHeight, room.dimensions.height);
  });
  
  // Calculate the available width and height
  const containerWidth = allRoomsContainer.clientWidth;
  const itemsPerRow = Math.min(state.rooms.length, Math.floor(containerWidth / 300));
  const availableWidth = (containerWidth / itemsPerRow) - 32; // Accounting for gap
  
  // Calculate the scale
  const widthScale = availableWidth / (maxWidth * state.gridSize);
  state.viewScale = Math.min(0.5, widthScale); // Cap at 0.5 for readability
  
  // Update all room canvases
  previewEls.forEach(previewEl => {
    const roomId = previewEl.getAttribute('data-room-id');
    const room = getRoomById(roomId);
    const canvas = previewEl.querySelector('.room-canvas');
    
    canvas.style.width = `${room.dimensions.width * state.gridSize}px`;
    canvas.style.height = `${room.dimensions.height * state.gridSize}px`;
    canvas.style.transform = `scale(${state.viewScale})`;
    canvas.style.transformOrigin = 'top left';
  });
};

// Exit View All Rooms mode
const exitViewAllRooms = () => {
  state.viewAllMode = false;
  
  // Hide view all controls
  document.getElementById('viewAllControls').style.display = 'none';
  
  // Show the regular canvas and hide the all rooms container
  document.getElementById('canvas').style.display = 'block';
  document.getElementById('allRoomsContainer').style.display = 'none';
  
  // Remove canvas container class for scrolling
  document.querySelector('.canvas-container').classList.remove('view-all-mode');
  
  // Reset view
  updateCanvasSize();
  renderGrowAreas();
};

// Auto arrange plants
const autoArrangePlants = () => {
  if (!state.selectedArea) return;
  
  const activeRoom = getActiveRoom();
  const area = activeRoom.growAreas.find(a => a.id === state.selectedArea);
  const areaPlants = activeRoom.plants.filter(p => p.areaId === state.selectedArea);
  
  if (!area || areaPlants.length === 0) return;
  
  // Calculate grid size for plants
  const areaWidth = area.width;
  const areaHeight = area.height;
  
  // Calculate number of plants in each dimension
  const plantCount = areaPlants.length;
  const aspectRatio = areaWidth / areaHeight;
  
  let cols = Math.round(Math.sqrt(plantCount * aspectRatio));
  let rows = Math.round(Math.sqrt(plantCount / aspectRatio));
  
  // Make sure we have enough cells
  while (cols * rows < plantCount) {
    cols++;
  }
  
  // Calculate spacing
  const spacingX = areaWidth / (cols + 1);
  const spacingY = areaHeight / (rows + 1);
  
  // Position plants
  let index = 0;
  for (let row = 0; row < rows && index < plantCount; row++) {
    for (let col = 0; col < cols && index < plantCount; col++) {
      const plant = areaPlants[index];
      const plantIndex = activeRoom.plants.findIndex(p => p.id === plant.id);
      
      if (plantIndex !== -1) {
        activeRoom.plants[plantIndex] = {
          ...activeRoom.plants[plantIndex],
          position: {
            x: (col + 1) * spacingX - state.gridSize / 4,
            y: (row + 1) * spacingY - state.gridSize / 4
          }
        };
      }
      
      index++;
    }
  }
  
  renderGrowAreas();
};

// Change active tab
const changeTab = (tabId) => {
  // Hide all tab contents
  document.querySelectorAll('.tab-content').forEach(tab => {
    tab.classList.remove('active');
  });
  
  // Deactivate all tabs
  document.querySelectorAll('.tab').forEach(tab => {
    tab.classList.remove('active');
  });
  
  // Show selected tab content
  document.getElementById(`${tabId}-tab`).classList.add('active');
  
  // Activate selected tab
  document.querySelector(`.tab[data-tab="${tabId}"]`).classList.add('active');
  
  state.activeTab = tabId;
};

// Handle tag input
const handleTagInput = (e) => {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault();
    
    const value = e.target.value.trim();
    if (value && !state.currentTags.includes(value)) {
      state.currentTags.push(value);
      e.target.value = '';
      renderTags();
    }
  }
};

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
  // Set initial canvas size
  updateCanvasSize();
  
  // Render room tabs
  renderRoomTabs();
  
  // Render breeding tags
  renderBreedingTags();
  
  // Event listeners for tools
  document.getElementById('selectTool').addEventListener('click', () => setTool('select'));
  document.getElementById('area4x4Tool').addEventListener('click', () => setTool('area4x4'));
  document.getElementById('area5x5Tool').addEventListener('click', () => setTool('area5x5'));
  document.getElementById('customAreaTool').addEventListener('click', () => {
    document.getElementById('customAreaModal').style.display = 'flex';
  });
  document.getElementById('plantTool').addEventListener('click', () => setTool('plant'));
  document.getElementById('multiplePlantsTool').addEventListener('click', () => setTool('multiplePlants'));
  document.getElementById('deleteBtn').addEventListener('click', deleteSelected);
  document.getElementById('showNamesBtn').addEventListener('click', toggleShowNames);
  document.getElementById('showDimensionsBtn').addEventListener('click', toggleShowDimensions);
  document.getElementById('roomSettingsBtn').addEventListener('click', () => {
    const activeRoom = getActiveRoom();
    document.getElementById('roomWidth').value = activeRoom.dimensions.width;
    document.getElementById('roomHeight').value = activeRoom.dimensions.height;
    document.getElementById('gridSnapSize').value = state.gridSnapSize;
    document.getElementById('roomSettingsModal').style.display = 'flex';
  });
  
  // View All Rooms button
  document.getElementById('viewAllRoomsBtn').addEventListener('click', viewAllRooms);
  
  // Fit to Screen button
  document.getElementById('fitToScreenBtn').addEventListener('click', fitRoomsToScreen);
  
  // Exit View All button
  document.getElementById('exitViewAllBtn').addEventListener('click', exitViewAllRooms);
  
  // Canvas event listeners
  document.getElementById('canvas').addEventListener('click', handleCanvasClick);
  
  // Use document for mouse move to handle dragging outside canvas
  document.addEventListener('mousemove', handleMouseMove);
  
  // Mouse up event listener (for ending drag operations)
  document.addEventListener('mouseup', () => {
    if (state.isDragging) {
      // Reset cursor styles
      document.body.style.cursor = '';
      
      // If in the middle of dragging, update the details panel
      if (state.dragTarget === 'area' || state.dragTarget === 'plant') {
        updateDetailsPanel();
        
        // Reset additional visual feedback
        const elements = state.dragTarget === 'area' 
          ? document.querySelectorAll('.grow-area') 
          : document.querySelectorAll('.plant');
          
        elements.forEach(el => {
          el.style.boxShadow = '';
          el.style.zIndex = '';
          el.style.cursor = '';
        });
      }
      
      state.isDragging = false;
      state.dragTarget = null;
      
      // Reset any highlighted drop areas
      document.querySelectorAll('.grow-area').forEach(area => {
        const activeRoom = getActiveRoom();
        const areaObj = activeRoom.growAreas.find(a => a.id === area.id.replace('area-', ''));
        if (areaObj) {
          area.style.borderStyle = 'solid';
          area.style.borderColor = areaObj.borderColor || '#16a34a';
          area.style.backgroundColor = areaObj.color || 'rgba(220, 252, 231, 0.7)';
        }
      });
      
      state.potentialDropArea = null;
    }
  });
  
  // Prevent page scrolling during drag operations
  document.addEventListener('dragover', (e) => {
    if (state.isDragging) {
      e.preventDefault();
    }
  });
  
  // Tab navigation
  document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
      const tabId = tab.getAttribute('data-tab');
      changeTab(tabId);
    });
  });
  
  // Add New Plant button
  document.getElementById('addNewPlantBtn').addEventListener('click', () => {
    // Set default position for plants added outside of the grow room
    const activeRoom = getActiveRoom();
    state.newPlant = {
      ...state.newPlant,
      position: { x: 0, y: 0 },
      areaId: activeRoom.growAreas.length > 0 ? activeRoom.growAreas[0].id : null
    };
    openPlantModal();
  });
  
  // Add First Plant Button (in inventory tab)
  const addFirstPlantBtn = document.getElementById('addFirstPlantBtn');
  if (addFirstPlantBtn) {
    addFirstPlantBtn.addEventListener('click', () => {
      changeTab('grow-room');
      setTool('plant');
    });
  }
  
  // Add Room button
  document.getElementById('addRoomBtn').addEventListener('click', showAddRoomModal);
  
  // Room type change
  document.getElementById('roomType').addEventListener('change', handleRoomTypeChange);
  
  // Plant modal event listeners
  document.getElementById('closePlantModalBtn').addEventListener('click', () => {
    document.getElementById('plantFormModal').style.display = 'none';
  });
  
  document.getElementById('cancelPlantBtn').addEventListener('click', () => {
    document.getElementById('plantFormModal').style.display = 'none';
  });
  
  document.getElementById('addPlantBtn').addEventListener('click', addPlant);
  
  // Multiple Plants modal event listeners
  document.getElementById('closeMultiplePlantsModalBtn').addEventListener('click', () => {
    document.getElementById('multiplePlantsModal').style.display = 'none';
  });
  
  document.getElementById('cancelMultiplePlantsBtn').addEventListener('click', () => {
    document.getElementById('multiplePlantsModal').style.display = 'none';
  });
  
  document.getElementById('addMultiplePlantsBtn').addEventListener('click', addMultiplePlants);
  
  // Tag input handling
  document.getElementById('plantTags').addEventListener('keydown', handleTagInput);
  
  // Color change modal event listeners
  document.getElementById('closeColorModalBtn').addEventListener('click', () => {
    document.getElementById('colorChangeModal').style.display = 'none';
  });
  
  document.getElementById('cancelColorChangeBtn').addEventListener('click', () => {
    document.getElementById('colorChangeModal').style.display = 'none';
  });
  
  document.getElementById('applyColorChangeBtn').addEventListener('click', applyColorChange);
  
  // Area color change modal event listeners
  document.getElementById('closeAreaColorModalBtn').addEventListener('click', () => {
    document.getElementById('areaColorChangeModal').style.display = 'none';
  });
  
  document.getElementById('cancelAreaColorChangeBtn').addEventListener('click', () => {
    document.getElementById('areaColorChangeModal').style.display = 'none';
  });
  
  document.getElementById('applyAreaColorChangeBtn').addEventListener('click', applyAreaColorChange);
  
  // Room settings modal event listeners
  document.getElementById('closeRoomSettingsBtn').addEventListener('click', () => {
    document.getElementById('roomSettingsModal').style.display = 'none';
  });
  
  document.getElementById('cancelRoomSettingsBtn').addEventListener('click', () => {
    document.getElementById('roomSettingsModal').style.display = 'none';
  });
  
  document.getElementById('applyRoomSettingsBtn').addEventListener('click', applyRoomSettings);
  
  // Custom area modal event listeners
  document.getElementById('closeCustomAreaBtn').addEventListener('click', () => {
    document.getElementById('customAreaModal').style.display = 'none';
  });
  
  document.getElementById('cancelCustomAreaBtn').addEventListener('click', () => {
    document.getElementById('customAreaModal').style.display = 'none';
  });
  
  document.getElementById('createCustomAreaBtn').addEventListener('click', createCustomArea);
  
  // Custom color preview update
  document.getElementById('customAreaColor').addEventListener('input', (e) => {
    document.getElementById('customAreaColorPreview').style.backgroundColor = e.target.value;
  });
  
  document.getElementById('customAreaColorOpacity').addEventListener('input', (e) => {
    const color = document.getElementById('customAreaColor').value;
    const opacity = e.target.value;
    document.getElementById('customAreaColorPreview').style.opacity = opacity;
  });
  
  // Add Room modal event listeners
  document.getElementById('closeAddRoomModalBtn').addEventListener('click', () => {
    document.getElementById('addRoomModal').style.display = 'none';
  });
  
  document.getElementById('cancelAddRoomBtn').addEventListener('click', () => {
    document.getElementById('addRoomModal').style.display = 'none';
  });
  
  document.getElementById('createRoomBtn').addEventListener('click', createRoom);
  
  // Room color preview update
  document.getElementById('roomColor').addEventListener('input', (e) => {
    document.getElementById('roomColorPreview').style.backgroundColor = e.target.value;
  });
  
  // Resize area modal event listeners
  document.getElementById('closeResizeAreaBtn').addEventListener('click', () => {
    document.getElementById('resizeAreaModal').style.display = 'none';
  });
  
  document.getElementById('cancelResizeAreaBtn').addEventListener('click', () => {
    document.getElementById('resizeAreaModal').style.display = 'none';
  });
  
  document.getElementById('applyResizeAreaBtn').addEventListener('click', applyResizeArea);
  
  // Action buttons
  document.getElementById('autoArrangePlantsBtn').addEventListener('click', autoArrangePlants);
  document.getElementById('changeColorBtn').addEventListener('click', showColorChangeModal);
  document.getElementById('changeAreaColorBtn').addEventListener('click', showAreaColorChangeModal);
  document.getElementById('resizeAreaBtn').addEventListener('click', showResizeAreaModal);
  
  document.getElementById('editPlantBtn').addEventListener('click', () => {
    if (!state.selectedPlant) return;
    
    const activeRoom = getActiveRoom();
    const plant = activeRoom.plants.find(p => p.id === state.selectedPlant);
    if (!plant) return;
    
    // Populate form with plant details
    document.getElementById('plantName').value = plant.name;
    document.getElementById('plantStrain').value = plant.strain || '';
    document.getElementById('plantBreeder').value = plant.breeder || '';
    document.getElementById('plantCategory').value = plant.category;
    document.getElementById('plantSex').value = plant.sex;
    document.getElementById('plantStage').value = plant.stage;
    document.getElementById('plantColor').value = plant.color;
    document.getElementById('plantColorPreview').style.backgroundColor = plant.color;
    document.getElementById('plantNotes').value = plant.notes || '';
    document.getElementById('plantQuantity').value = 1;
    
    // Set tags
    state.currentTags = [...(plant.tags || [])];
    renderTags();
    
    // Set breeding tags
    state.currentBreedingTags = [...(plant.breedingTags || [])];
    renderBreedingTags();
    
    // Change modal title
    document.querySelector('#plantFormModal .modal-title').textContent = 'Edit Plant';
    document.getElementById('addPlantBtn').textContent = 'Update Plant';
    
    // Replace Add button with Update button
    const addBtn = document.getElementById('addPlantBtn');
    addBtn.onclick = () => {
      const name = document.getElementById('plantName').value.trim();
      const strain = document.getElementById('plantStrain').value.trim();
      const breeder = document.getElementById('plantBreeder').value.trim();
      if (!name) return;
      
      const plantIndex = activeRoom.plants.findIndex(p => p.id === state.selectedPlant);
      if (plantIndex !== -1) {
        activeRoom.plants[plantIndex] = {
          ...activeRoom.plants[plantIndex],
          name,
          strain: strain || 'Unknown',
          breeder,
          category: document.getElementById('plantCategory').value,
          sex: document.getElementById('plantSex').value,
          stage: document.getElementById('plantStage').value,
          color: document.getElementById('plantColor').value,
          tags: state.currentTags,
          breedingTags: state.currentBreedingTags,
          notes: document.getElementById('plantNotes').value
        };
        
        document.getElementById('plantFormModal').style.display = 'none';
        
        // Reset button and handler
        addBtn.textContent = 'Add Plant';
        addBtn.onclick = addPlant;
        
        renderGrowAreas();
        renderPlantList();
        updateDetailsPanel();
      }
    };
    
    document.getElementById('plantFormModal').style.display = 'flex';
  });
  
  // Plant color preview update
  document.getElementById('plantColor').addEventListener('input', () => {
    document.getElementById('plantColorPreview').style.backgroundColor = document.getElementById('plantColor').value;
  });
  
  // Multiple plant color preview update
  document.getElementById('multiplePlantColor').addEventListener('input', () => {
    document.getElementById('multiplePlantColorPreview').style.backgroundColor = document.getElementById('multiplePlantColor').value;
  });
  
  // New plant color preview update
  document.getElementById('newPlantColor').addEventListener('input', () => {
    document.getElementById('newColorPreview').style.backgroundColor = document.getElementById('newPlantColor').value;
  });
  
  // Auto update plant color when category changes
  document.getElementById('plantCategory').addEventListener('change', (e) => {
    const color = getPlantColor(e.target.value);
    document.getElementById('plantColor').value = color;
    document.getElementById('plantColorPreview').style.backgroundColor = color;
  });
  
  // Auto update multiple plant color when category changes
  document.getElementById('multiplePlantCategory').addEventListener('change', (e) => {
    const color = getPlantColor(e.target.value);
    document.getElementById('multiplePlantColor').value = color;
    document.getElementById('multiplePlantColorPreview').style.backgroundColor = color;
  });
  
  // Search functionality
  document.getElementById('searchPlant').addEventListener('input', (e) => {
    const searchText = e.target.value.toLowerCase();
    const plantItems = document.querySelectorAll('.plant-item');
    
    plantItems.forEach(item => {
      const nameEl = item.querySelector('.plant-name');
      const strainEl = item.querySelector('.plant-strain');
      
      if (!nameEl || !strainEl) return;
      
      const name = nameEl.textContent.toLowerCase();
      const strain = strainEl.textContent.toLowerCase();
      
      if (name.includes(searchText) || strain.includes(searchText)) {
        item.style.display = 'flex';
      } else {
        item.style.display = 'none';
      }
    });
  });
  
  // Initialize plant list
  renderPlantList();
});
