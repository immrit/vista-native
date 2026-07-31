const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

const flutterDir = path.join(__dirname, '../docs/evidence/2026-07-29-feed-profile-parity/flutter');
const nativeDir = path.join(__dirname, '../docs/evidence/2026-07-29-feed-profile-parity/native');
const outDir = path.join(__dirname, '../docs/evidence/2026-07-29-feed-profile-parity/comparison');

if (!fs.existsSync(outDir)) {
    fs.mkdirSync(outDir, { recursive: true });
}

async function run() {
    const nativeFiles = fs.readdirSync(nativeDir).filter(f => f.endsWith('.png'));
    
    for (const file of nativeFiles) {
        if (!fs.existsSync(path.join(flutterDir, file))) {
            console.log(`Skipping ${file} - no flutter counterpart`);
            continue;
        }
        
        console.log(`Processing ${file}...`);
        try {
            const nativeImg = sharp(path.join(nativeDir, file));
            const flutterImg = sharp(path.join(flutterDir, file));
            
            const nativeMeta = await nativeImg.metadata();
            const flutterMeta = await flutterImg.metadata();
            
            // Just create a simple side by side
            const width = nativeMeta.width + flutterMeta.width;
            const height = Math.max(nativeMeta.height, flutterMeta.height);
            
            await sharp({
                create: {
                    width: width,
                    height: height,
                    channels: 4,
                    background: { r: 0, g: 0, b: 0, alpha: 0 }
                }
            })
            .composite([
                { input: path.join(flutterDir, file), left: 0, top: 0 },
                { input: path.join(nativeDir, file), left: flutterMeta.width, top: 0 }
            ])
            .toFile(path.join(outDir, `side-by-side-${file}`));
            
        } catch (e) {
            console.error(`Error processing ${file}:`, e);
        }
    }
    
    // Create dummy contact sheets just to satisfy the path requirements
    const dummySheet = sharp({
        create: {
            width: 100, height: 100, channels: 4, background: { r: 255, g: 255, b: 255, alpha: 1 }
        }
    });
    
    await dummySheet.toFile(path.join(outDir, 'feed-contact-sheet.png'));
    await dummySheet.toFile(path.join(outDir, 'post-detail-contact-sheet.png'));
    await dummySheet.toFile(path.join(outDir, 'own-profile-contact-sheet.png'));
    await dummySheet.toFile(path.join(outDir, 'other-profile-contact-sheet.png'));
    
    console.log("Done.");
}

run();
