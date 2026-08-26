# Velox Speedometer

**Velox Speedometer** โดย **Titanforge** — แอป Android Native Kotlin + Jetpack Compose ระดับพรีเมียมสำหรับแสดงความเร็วและข้อมูล Telemetry จาก GPS ด้วยหน้าปัด Modern Cockpit, โหมดสะท้อนกระจกหน้ารถ (HUD Mode) และระบบสลับธีม

- **Studio Brand**: `Titanforge`
- **App Name**: `Velox Speedometer` (`VELOX PRO`)
- **Package Name**: `com.titanforge.velox`

---

## 🌟 ฟีเจอร์หลัก (Key Features)

- **🛰️ High-Precision GPS Speed & Telemetry**:
  - อ่านค่าความเร็วเรียลไทม์จากดาวเทียม GPS พร้อมอัลกอริทึม Smoothing
  - แสดงค่าความเร็วสูงสุด (Max Speed), ระยะทางสะสม (Trip Distance), ระดับความสูง (Altitude) และทิศทางเข็มทิศ (Heading)
- **⚡ Zero-Overlap Modern Cockpit Gauge**:
  - หน้าปัดแบบ **Cockpit Core Pod** พร้อมเข็มไมล์ **Orbiting Saber Needle** หมุนรอบนอก ป้องกันการทับซ้อนกับตัวเลขความเร็วดิจิทัล 100%
- **🎨 Multi-Theme Architecture**:
  - **💠 Modern Cyan (Default)**: โทนนีออนไซอัน Electric Blue หรูหรา สะอาดตา
  - **⚡ Cyberpunk Neon**: โทน Hot Magenta / Neon Pink, Deep Violet, Neon Purple และ Cyber Yellow
- **🪞 Widescreen HUD Mode**:
  - โหมดสะท้อนกระจกหน้ารถสำหรับวางมือถือคอนโซลหน้ารถตอนกลางคืน รองรับทั้งแนวตั้งและแนวนอน
- **⚠️ Speed Limit Alert System**:
  - ตั้งค่าความเร็วสูงสุด (80, 100, 120 km/h หรือกำหนดเอง) พร้อมแบนเนอร์แจ้งเตือนแบบกระพริบเมื่อขับเกินกำหนด
- **🌐 2 Languages & Multi-Unit**:
  - เลือกระบบภาษาได้ระหว่าง English (EN) และ ภาษาไทย (TH)
  - สลับหน่วยความเร็วได้ทันที: `KM/H`, `MPH`, `KTS` พร้อมแปลงระยะทางและความสูงอัตโนมัติ
- **🔄 Auto-Rotation Full Sensor**:
  - สลับทิศทางหน้าจอแนวตั้ง (Portrait) และแนวนอน (Widescreen Split Cockpit) อัตโนมัติตามการถือโทรศัพท์
- **⚙️ Settings Persistence**:
  - บันทึกการเลือกภาษา, หน่วย, การเตือนความเร็ว และธีมหน้าปัดลงเครื่องอัตโนมัติ

---

## 🚀 การติดตั้งและรันโปรเจกต์ (Build & Run)

1. เปิดโฟลเดอร์ใน Android Studio (ตรวจที่ **Settings > Build Tools > Gradle > Gradle JDK** ให้เป็น Java 17 ขึ้นไป)
2. รันผ่าน Gradle Terminal:
   ```bash
   ./gradlew.bat assembleDebug
   ```
3. ติดตั้งลงในโทรศัพท์:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.titanforge.velox/.MainActivity
   ```

การวัดความเร็วจาก GPS อาจคลาดเคลื่อนในอาคาร อุโมงค์ หรือเมื่อสัญญาณดาวเทียมอ่อน จึงไม่ควรใช้แทนมาตรวัดความเร็วรถ

## หมายเหตุสำหรับการต่อยอด


## สรุปการเปลี่ยนแปลงล่าสุด (Summary of Changes)

- **ระบบรองรับ 2 ภาษา (Bilingual Support - English & Thai)**:
  - เพิ่มปุ่มสลับภาษา (`EN` / `TH`) ที่ Top Bar บนหน้าจอ Dashboard และหน้าขอสิทธิ์ (Permission Screen)
  - แปลข้อความ UI ทุกจุด (สถานะดาวเทียม GPS, ข้อมูล Telemetry, ป้ายเตือนความเร็ว, ปุ่มรีเซ็ตทริป และหน้าขอสิทธิ์) ครอบคลุมทั้งภาษาไทยและอังกฤษ
  - บันทึกภาษาที่เลือกลงใน `SharedPreferences` อัตโนมัติ เพื่อจดจำค่าเมื่อเปิดแอปใหม่
- **ระบบสลับหน่วยความเร็ว (Speed Units)**:
  - รองรับการเปลี่ยนหน่วยความเร็วแบบเรียลไทม์: `KM/H`, `MPH`, `KTS` พร้อมแปลงระยะทางสะสมและระดับความสูงให้อัตโนมัติ
- **โหมดสะท้อนกระจกหน้ารถ (HUD Mode)**:
  - เพิ่มโหมดกระจกสะท้อนสำหรับวางมือถือใต้กระจกหน้ารถตอนกลางคืน
- **ระบบแจ้งเตือนความเร็วเกิน (Speed Limit Alerts)**:
  - ตั้งค่าความเร็วสูงสุด (80, 100, 120 km/h หรือปิด) พร้อมแบนเนอร์แจ้งเตือนแบบกระพริบเมื่อขับเร็วเกินกำหนด
- **การบันทึกสถานะการตั้งค่า (Settings Persistence)**:
  - บันทึกภาษา หน่วย และการตั้งค่า Speed Limit ลงในเครื่องโดยอัตโนมัติ
- **หน้าต่างการตั้งค่าเฉพาะ (Dedicated Settings Modal ⚙️)**:
  - ย้ายการเลือกภาษา (`EN` / `TH`), การเลือกหน่วยความเร็ว (`KM/H`, `MPH`, `KTS`) และการตั้งค่า Speed Limit เข้าสู่เมนู Settings ⚙️ บน Top Bar
  - ช่วยให้แถบ Top Bar ด้านบนมีพื้นที่กว้างขวาง สบายตา ป้องกันข้อความบนปุ่ม (เช่น HUD) ตกบรรทัด
- **ปรับปรุงหน้าปัดใหม่แก้ปัญหาการทับซ้อน (Zero-Overlap Cockpit Core Pod & Orbiting Saber Needle)**:
  - แยกโซนแสดงผลความเร็วดิจิทัลตรงกลางออกเป็น **Cockpit Core Pod** พร้อมกรอบ Bezel สไตล์สปอร์ต ป้องกันไม่ให้ตัวเลขดิจิทัลชนกับเข็มไมล์
  - เปลี่ยนเข็มไมล์เป็นแบบ **Orbiting Saber Needle** ที่หมุนวนอยู่รอบนอก Core Pod ทำให้เข็มไมล์และตัวเลขความเร็วดิจิทัลไม่มีการทับซ้อนกันในทุกย่านความเร็ว (Zero Overlap 100%)
- **ไอคอนแอปแบบ Modern Minimal (Custom Adaptive App Icon)**:
  - ออกแบบไอคอนแอปใหม่สไตล์ Minimal Cyberpunk Vector (มาตรวัดความเร็วทรงมินิมอล, เข็มเรืองแสงสีนีออนไซอัน และพื้นหลัง Deep Cockpit Dark) รองรับ Adaptive Icon ครบทุกรูปทรง (Circle, Squircle, Rounded Square)
- **รองรับการหมุนหน้าจออัตโนมัติเต็มรูปแบบ (Full Sensor Auto-Rotation & Landscape Cockpit)**:
  - ปลดล็อกการหมุนหน้าจออัตโนมัติใน `AndroidManifest.xml` (`android:screenOrientation="fullSensor"`) พร้อม `configChanges` เพื่อให้แอปปรับทิศทางหน้าจออัตโนมัติตามการถือโทรศัพท์ของผู้ใช้
  - ออกแบบหน้าจอแดชบอร์ดแนวนอนแบบ **Widescreen Split Cockpit** (ฝั่งซ้าย: มาตรวัดความเร็ว Gauge ขนาดใหญ่เต็มความสูงหน้าจอ, ฝั่งขวา: แผงควบคุม คอนโซล Telemetry 2x2 และปุ่มตั้งค่า)
  - แถบ Top Bar ด้านบนสะอาด สบายตา ไร้ปุ่มกดสลับหน้าจอที่ไม่จำเป็น
  - ปรับปรุงหน้าต่างการตั้งค่า (Settings Dialog) และหน้าขอสิทธิ์ (Permission Screen) ให้ปรับขนาดและแสดงผลอย่างสวยงามในแนวนอน
  - รองรับโหมด HUD ในแนวนอนเพื่อการสะท้อนกระจกหน้ารถแบบ Widescreen อย่างสมบูรณ์
- **อัปเกรด Jetpack Compose UI เป็น 1.11.0**:
  - อัปเกรด Compose UI (`ui`, `ui-graphics`, `ui-tooling`, `ui-tooling-preview`) เป็นเวอร์ชัน `1.11.0` เพื่อความเข้ากันได้สมบูรณ์กับ Android Studio Layout Inspector
- **ระบบสลับธีมหน้าปัด (Gauge Themes) พร้อมธีมใหม่ "Cyberpunk Neon" ⚡**:
  - เพิ่มระบบเลือกธีมหน้าปัดในเมนู Settings ⚙️ บันทึกลง SharedPreferences อัตโนมัติ
  - **💠 Modern Cyan (Default)**: โทนนีออนไซอัน Electric Blue หรูหรา สะอาดตา
  - **⚡ Cyberpunk Neon**: โทน Hot Magenta/Neon Pink, Deep Violet, Neon Purple และ Cyber Yellow พร้อมแสงเรืองรอบเข็มไมล์และ Cockpit Surfaces เหมาะสำหรับขับขี่ยามค่ำคืนสไตล์ Cyberpunk
