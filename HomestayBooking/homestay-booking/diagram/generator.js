const fs = require('fs');
const path = require('path');

const entityDir = 'd:\\FLearning\\DU AN CA NHAN\\BookingHome\\HomestayBooking\\homestay-booking\\Backend\\src\\main\\java\\com\\homestay\\entity';
const outputDir = 'd:\\FLearning\\DU AN CA NHAN\\BookingHome\\HomestayBooking\\homestay-booking\\diagram';
const files = fs.readdirSync(entityDir).filter(f => f.endsWith('.java'));

const allEntities = [];

function mapType(javaType) {
    javaType = javaType.replace(/<.*>/, '');
    const map = {
        'String': 'varchar',
        'Integer': 'int',
        'int': 'int',
        'BigDecimal': 'decimal',
        'LocalDateTime': 'datetime',
        'LocalDate': 'date',
        'UUID': 'uuid',
        'boolean': 'boolean',
        'Boolean': 'boolean',
        'List': 'json',
    };
    return map[javaType] || 'varchar';
}

function toSnakeCase(str) {
    return str.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`).replace(/^_/, '');
}

files.forEach(file => {
    const content = fs.readFileSync(path.join(entityDir, file), 'utf-8');
    if (!content.includes('@Entity')) return;

    let classNameMatch = content.match(/public\s+class\s+(\w+)/);
    if (!classNameMatch) return;
    let className = classNameMatch[1];
    
    let tableName = className.toLowerCase() + 's';
    let tableMatch = content.match(/@Table\([\s\S]*?name\s*=\s*"([^"]+)"/);
    if (tableMatch) tableName = tableMatch[1];

    const fields = [];
    const lines = content.split('\n');
    let currentAnnotations = [];
    
    for (let i = 0; i < lines.length; i++) {
        let line = lines[i].trim();
        if (line.startsWith('@')) {
            currentAnnotations.push(line);
        } else if (line.match(/private\s+([\w<>]+)\s+(\w+)\s*(?:=|;)/)) {
            let match = line.match(/private\s+([\w<>]+)\s+(\w+)\s*(?:=|;)/);
            let javaType = match[1];
            let name = match[2];
            
            let isId = currentAnnotations.some(a => a.includes('@Id'));
            let isManyToOne = currentAnnotations.some(a => a.includes('@ManyToOne'));
            let isOneToOne = currentAnnotations.some(a => a.includes('@OneToOne') && !a.includes('mappedBy'));
            let joinColumn = currentAnnotations.find(a => a.includes('@JoinColumn'));
            let isOneToMany = currentAnnotations.some(a => a.includes('@OneToMany'));
            
            let dbName = toSnakeCase(name);
            if (joinColumn) {
                let jcMatch = joinColumn.match(/name\s*=\s*"([^"]+)"/);
                if (jcMatch) dbName = jcMatch[1];
            } else if (isManyToOne || isOneToOne) {
                dbName = dbName + '_id';
            }

            if (!isOneToMany && !currentAnnotations.some(a => a.includes('mappedBy'))) {
                fields.push({
                    name: dbName,
                    type: mapType(javaType),
                    isId: isId,
                    isFk: isManyToOne || isOneToOne,
                    ref: (isManyToOne || isOneToOne) ? javaType : null
                });
            }
            currentAnnotations = [];
        } else if (line && !line.startsWith('//') && !line.startsWith('public') && !line.startsWith('}')) {
             if (line.indexOf(';') !== -1) currentAnnotations = [];
        }
    }
    
    allEntities.push({
        name: className,
        tableName: tableName,
        fields: fields
    });
});

const domains = [
    {
        filename: 'db-schema-booking.html',
        title: 'Booking & Billing Flow',
        entities: ['Booking', 'User', 'Room', 'Invoice', 'Payment', 'Contract', 'Promotion', 'BookingCheckVerification']
    },
    {
        filename: 'db-schema-property.html',
        title: 'Property & Inventory Flow',
        entities: ['Property', 'Floor', 'Room', 'RoomImage', 'RoomStatusBlock', 'Review', 'ManagerPropertyAssignment', 'EmployeePropertyAssignment']
    },
    {
        filename: 'db-schema-operations.html',
        title: 'Operations & Maintenance Flow',
        entities: ['RoomInspection', 'HousekeepingTask', 'MaintenanceTicket', 'DamageReport', 'DamageItem', 'ChecklistItemDefinition', 'InspectionChecklistAnswer', 'Complaint', 'Attachment', 'Room']
    },
    {
        filename: 'db-schema-system.html',
        title: 'System & Users Flow',
        entities: ['User', 'Notification', 'ActivityLog', 'OutboxEvent', 'RefreshToken', 'SystemSetting']
    }
];

domains.forEach(domain => {
    let entities = JSON.parse(JSON.stringify(allEntities.filter(e => domain.entities.includes(e.name))));
    
    const cols = 4;
    const colWidth = 260;
    const gapX = 100;
    const gapY = 80;
    const startX = 40;
    const startY = 120;
    
    const colHeights = Array(cols).fill(startY);
    
    entities.sort((a, b) => domain.entities.indexOf(a.name) - domain.entities.indexOf(b.name));
    
    entities.forEach(ent => {
        let minCol = 0;
        for (let i = 1; i < cols; i++) {
            if (colHeights[i] < colHeights[minCol]) minCol = i;
        }
        
        ent.x = startX + minCol * (colWidth + gapX);
        ent.y = colHeights[minCol];
        ent.h = 48 + ent.fields.length * 20;
        
        colHeights[minCol] += ent.h + gapY;
    });
    
    const maxH = Math.max(...colHeights) + 100;
    const maxW = startX + cols * (colWidth + gapX);
    
    let svg = `<svg viewBox="0 0 ${maxW} ${maxH}" xmlns="http://www.w3.org/2000/svg">\n`;
    svg += `<defs>
              <pattern id="dots" width="22" height="22" patternUnits="userSpaceOnUse">
                <circle cx="1" cy="1" r="0.9" fill="rgba(45,49,66,0.10)"/>
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="#f5f5f5"/>
            <rect width="100%" height="100%" fill="url(#dots)" opacity="0.55"/>\n`;
    
    entities.forEach(ent => {
        ent.fields.forEach((f, idx) => {
            if (f.isFk && f.ref) {
                let targetEnt = entities.find(e => e.name === f.ref);
                if (targetEnt) {
                    let y1 = ent.y + 60 + idx * 20 - 5;
                    let x1 = ent.x + colWidth;
                    let isRight = false;
                    
                    if (targetEnt.x < ent.x) {
                        x1 = ent.x;
                        isRight = true;
                    }
                    
                    let y2 = targetEnt.y + 60;
                    let x2 = targetEnt.x;
                    
                    if (isRight) {
                        x2 = targetEnt.x + colWidth;
                    }
                    
                    let ctrlX1 = isRight ? x1 - 100 : x1 + 100;
                    let ctrlX2 = isRight ? x2 + 100 : x2 - 100;
                    
                    svg += `<path d="M ${x1},${y1} C ${ctrlX1},${y1} ${ctrlX2},${y2} ${x2},${y2}" fill="none" stroke="rgba(45,49,66,0.4)" stroke-width="1.5" />\n`;
                }
            }
        });
    });
    
    entities.forEach(ent => {
        let isFocal = (idx = domain.entities.indexOf(ent.name)) === 0;
        let color = isFocal ? 'rgba(235,108,54,0.04)' : '#ffffff';
        let stroke = isFocal ? '#eb6c36' : '#2d3142';
        let text = isFocal ? '#eb6c36' : '#4f5d75';
        let headFill = isFocal ? 'rgba(235,108,54,0.10)' : 'rgba(45,49,66,0.04)';
        let lineStroke = isFocal ? 'rgba(235,108,54,0.40)' : 'rgba(45,49,66,0.22)';
        
        svg += `<g id="${ent.name}">\n`;
        svg += `<rect x="${ent.x}" y="${ent.y}" width="${colWidth}" height="${ent.h}" rx="6" fill="${color}" stroke="${stroke}" stroke-width="1"/>\n`;
        svg += `<rect x="${ent.x}" y="${ent.y}" width="${colWidth}" height="40" rx="6" fill="${headFill}" stroke="none"/>\n`;
        svg += `<rect x="${ent.x}" y="${ent.y+32}" width="${colWidth}" height="8" fill="${headFill}"/>\n`;
        svg += `<line x1="${ent.x}" y1="${ent.y+40}" x2="${ent.x+colWidth}" y2="${ent.y+40}" stroke="${lineStroke}" stroke-width="1"/>\n`;
        
        let typeLabel = isFocal ? 'TABLE · AGGREGATE ROOT' : 'TABLE';
        svg += `<text x="${ent.x+16}" y="${ent.y+16}" fill="${text}" font-size="8" font-family="'Geist Mono', monospace" letter-spacing="0.14em">${typeLabel}</text>\n`;
        svg += `<text x="${ent.x+16}" y="${ent.y+32}" fill="#2d3142" font-size="14" font-weight="600" font-family="'Geist', sans-serif">${ent.tableName}</text>\n`;
        
        ent.fields.forEach((f, idx) => {
            let fy = ent.y + 60 + idx * 20;
            let prefix = f.isId ? '# ' : (f.isFk ? '→ ' : '');
            svg += `<text x="${ent.x+16}" y="${fy}" fill="#2d3142" font-size="10" font-family="'Geist Mono', monospace">${prefix}${f.name}</text>\n`;
            svg += `<text x="${ent.x+colWidth-16}" y="${fy}" fill="#4f5d75" font-size="10" font-family="'Geist Mono', monospace" text-anchor="end">${f.type}</text>\n`;
        });
        svg += `</g>\n`;
    });
    
    svg += `</svg>`;
    
    const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Homestay Booking · ${domain.title}</title>
  <link href="https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&family=Geist:wght@400;500;600&family=Geist+Mono:wght@400;500;600&display=swap" rel="stylesheet">
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    :root {
      --color-paper:   #f5f5f5;
      --color-ink:     #2d3142;
      --color-muted:   #4f5d75;
      --color-accent:  #eb6c36;
      --font-sans:     'Geist', system-ui, sans-serif;
      --font-serif:    'Instrument Serif', serif;
      --font-mono:     'Geist Mono', ui-monospace, monospace;
    }
    body {
      font-family: var(--font-sans);
      background: var(--color-paper);
      color: var(--color-ink);
      min-height: 100vh;
      display: flex;
      align-items: flex-start;
      justify-content: center;
      padding: 3rem 2rem;
    }
    .frame { max-width: 1400px; width: 100%; }
    .eyebrow {
      font-family: var(--font-mono);
      font-size: 0.66rem;
      font-weight: 500;
      letter-spacing: 0.18em;
      text-transform: uppercase;
      color: var(--color-muted);
      margin-bottom: 0.5rem;
    }
    h1 {
      font-family: var(--font-serif);
      font-size: clamp(1.5rem, 2.4vw + 0.75rem, 2rem);
      font-weight: 400;
      letter-spacing: -0.02em;
      line-height: 1.15;
      color: var(--color-ink);
      margin-bottom: 1.5rem;
    }
    svg { width: 100%; display: block; }
  </style>
</head>
<body>
  <div class="frame">
    <p class="eyebrow">ER / DATA MODEL · ${domain.title}</p>
    <h1>Homestay Booking · ${domain.title}</h1>
    ${svg}
  </div>
</body>
</html>`;
    
    fs.writeFileSync(path.join(outputDir, domain.filename), html);
    console.log('Generated:', domain.filename);
});

// Clean up old file
if (fs.existsSync(path.join(outputDir, 'booking-table-design.html'))) {
    fs.unlinkSync(path.join(outputDir, 'booking-table-design.html'));
    console.log('Removed old booking-table-design.html');
}
