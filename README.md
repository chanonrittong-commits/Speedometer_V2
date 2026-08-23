# Speedometer

แอป Android Native Kotlin + Jetpack Compose สำหรับแสดงความเร็วจาก GPS ของโทรศัพท์

## สิ่งที่มีในตัวอย่าง

- ขอสิทธิ์ตำแหน่งแบบ runtime
- อ่านค่า `Location.speed` จาก GPS และแปลงเป็น km/h, mph, knots
- เลือกระบบภาษาได้ระหว่าง English (EN) และ ภาษาไทย (TH)
- Smooth ค่าเพื่อลดการกระโดดของตัวเลข
- หยุดอัปเดตตำแหน่งอัตโนมัติเมื่อแอปไม่อยู่ด้านหน้า
- หน้าปัด Compose พร้อม needle, HUD Mode และระบบแจ้งเตือนจำกัดความเร็ว

## เปิดและรัน

1. เปิดโฟลเดอร์นี้ใน Android Studio และตรวจที่ **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK** ว่าเลือก `Embedded JDK` (17 ขึ้นไป)
2. ติดตั้ง Android SDK Platform 35 หากยังไม่มี
3. กด **Sync Project with Gradle Files** แล้วรันบนโทรศัพท์จริง
4. อนุญาต **Precise location** และเปิด GPS

โปรเจกต์ตั้ง target bytecode เป็น Java 17 ทั้ง Kotlin และ Java จึงใช้งาน Gradle JDK 17 หรือ 21 ได้โดยไม่ทำให้ target ของทั้งสองภาษาไม่ตรงกัน

หรือรันบน Windows จาก Terminal ด้วย `./gradlew.bat assembleDebug` (ครั้งแรก Gradle จะดาวน์โหลด dependencies)

การวัดความเร็วจาก GPS อาจคลาดเคลื่อนในอาคาร อุโมงค์ หรือเมื่อสัญญาณดาวเทียมอ่อน จึงไม่ควรใช้แทนมาตรวัดความเร็วรถ

## หมายเหตุสำหรับการต่อยอด

เวอร์ชันนี้จงใจทำงานเฉพาะขณะที่หน้าจอแอปเปิดอยู่ หากต้องการบันทึกเส้นทางหรือทำงานขณะล็อกหน้าจอ ให้เพิ่ม Foreground Service พร้อม notification ที่มองเห็นได้

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
- **ไอคอนแอปแบบ Modern Minimal (Custom Adaptive App Icon)**:
  - ออกแบบไอคอนแอปใหม่สไตล์ Minimal Cyberpunk Vector (มาตรวัดความเร็วทรงมินิมอล, เข็มเรืองแสงสีนีออนไซอัน และพื้นหลัง Deep Cockpit Dark) รองรับ Adaptive Icon ครบทุกรูปทรง (Circle, Squircle, Rounded Square)
- **ปรับแต่งขอบหน้าจอ Edge-to-Edge & System Insets**:
  - รองรับ `enableEdgeToEdge()` และ `statusBarsPadding()` / `navigationBarsPadding()` เพื่อป้องกันไม่ให้ส่วนหัวของแอปและปุ่มควบคุมชนหรือซ้อนทับกับ Notification Bar / Camera Cutout และแถบ Gesture ด้านล่าง


