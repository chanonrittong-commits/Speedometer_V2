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
