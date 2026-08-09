package org.example


import kotlin.test.Test
import kotlin.test.assertEquals


class WorkshopTest {

    // --- Tests for Workshop #1: Unit Converter ---

    // celsius input: 20.0
    // expected output: 68.0
    @Test
    fun `test celsiusToFahrenheit with positive value`() {
        // Arrange: ตั้งค่า input และผลลัพธ์ที่คาดหวัง
        val celsiusInput = 20.0
        val expectedFahrenheit = 68.0

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        val actualFahrenheit = celsiusToFahrenheit(celsiusInput)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertEquals(expectedFahrenheit, actualFahrenheit, 0.001, "20°C should be 68°F")
    }

    // celsius input: 0.0
    // expected output: 32.0
    @Test
    fun `test celsiusToFahrenheit with zero`() {
 val celsiusInput = 0.0
        val expectedFahrenheit = 32.0

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        val actualFahrenheit = celsiusToFahrenheit(celsiusInput)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertEquals(expectedFahrenheit, actualFahrenheit, 0.001, "0°C should be 32°F")
    }

    // celsius input: -10.0
    // expected output: 14.0
    @Test
    fun `test celsiusToFahrenheit with negative value`() {
val celsiusInput = -10.0
        val expectedFahrenheit = 14.0

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        val actualFahrenheit = celsiusToFahrenheit(celsiusInput)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertEquals(expectedFahrenheit, actualFahrenheit, 0.001, "-10°C should be 14°F")
    }

    // test for kilometersToMiles function
    // kilometers input: 1.0
    // expected output: 0.621371
    @Test
    fun `test kilometersToMiles with one kilometer`() {
   val kilometersInput = 1.0
        val expectedMiles = 0.621371

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        val actualMiles = kilometersToMiles(kilometersInput)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertEquals(expectedMiles, actualMiles, 0.001, "1km should be 0.621371 miles")
    }

    // --- Tests for Workshop #1: Unit Converter End ---

    // --- Tests for Workshop #2: Data Analysis Pipeline ---
    // ทำการแก้ไขไฟล์ Workshop2.kt ให้มีฟังก์ชันที่ต้องการทดสอบ
    // เช่น ฟังก์ชันที่คำนวณผลรวมราคาสินค้า Electronics ที่ราคา > 500 บาท
    // ในที่นี้จะสมมุติว่ามีฟังก์ชันชื่อ calculateTotalElectronicsPriceOver500 ที่รับ List<Product> และคืนค่า Double
    // จงเขียน test cases สำหรับฟังก์ชันนี้ โดยตรวจสอบผลรวมราคาสินค้า Electronics ที่ราคา > 500 บาท
      @Test
    fun `test calculateTotalElectronicsPriceOver500 with sample data`() {
        // Arrange: เตรียมข้อมูลทดสอบ
        val products = listOf(
            Product("Laptop", 1200.0, "Electronics"),
            Product("Mouse", 25.0, "Electronics"),
            Product("Keyboard", 75.0, "Electronics"),
            Product("T-shirt", 20.0, "Apparel")
        )
        val expectedTotal = 1200.0  // มีเพียง Laptop ที่เป็น Electronics และราคาสูงกว่า 500

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        val actualTotal = calculateTotalElectronicsPriceOver500(products)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertEquals(expectedTotal, actualTotal, 0.001, "Should calculate total electronics price over 500 correctly")
    }

    @Test
    fun `test calculateTotalElectronicsPriceOver500 with no matching items`() {
        // Arrange: เตรียมข้อมูลทดสอบ - สินค้า Electronics ทั้งหมดราคาต่ำกว่า 500 บาท
        val products = listOf(
            Product("Mouse", 25.0, "Electronics"),
            Product("Keyboard", 75.0, "Electronics")
        )
        val expectedTotal = 0.0  // ไม่มีสินค้าที่เข้าเกณฑ์

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        val actualTotal = calculateTotalElectronicsPriceOver500(products)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertEquals(expectedTotal, actualTotal, 0.001, "Should return 0 when no electronics items are over 500 baht")
    }

    @Test
    fun `test calculateTotalElectronicsPriceOver500 with empty list`() {
        // Arrange: เตรียมข้อมูลทดสอบ - รายการสินค้าว่างเปล่า
        val products = emptyList<Product>()
        val expectedTotal = 0.0  // ผลรวมของรายการว่างเปล่าควรเป็น 0

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        val actualTotal = calculateTotalElectronicsPriceOver500(products)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertEquals(expectedTotal, actualTotal, 0.001, "Should return 0 for an empty product list")
    }

    @Test
    fun `test calculateTotalElectronicsPriceOver500 with mixed currencies`() {
        // Arrange: เตรียมข้อมูลทดสอบ - มีสินค้าจากหมวดหมู่ Music
        val products = listOf(
            Product("Laptop", 1500.0, "Electronics"),
            Product("Guitar", 800.0, "Music")
        )
        val expectedTotal = 1500.0  // Guitar ไม่ควรถูกนับเพราะหมวดหมู่ไม่ใช่ Electronics

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        val actualTotal = calculateTotalElectronicsPriceOver500(products)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertEquals(expectedTotal, actualTotal, 0.001, "Should only include Electronics category items")
    }
   @Test
fun `test count electronics items over 500`() {
    val products = listOf(
        Product("Laptop", 35000.0, "Electronics"),
        Product("Smartphone", 25000.0, "Electronics"),
        Product("T-shirt", 450.0, "Apparel"),
        Product("Keyboard", 499.0, "Electronics")  // ราคาไม่เกิน 500
    )
    val expectedCount = 2  // Laptop + Smartphone เท่านั้นที่ > 500

    val actualCount = products
        .filter { it.category == "Electronics" }
        .filter { it.price > 500 }
        .count()

    assertEquals(expectedCount, actualCount, "Should count only Electronics items over 500 baht")
}



    // --- Tests for Workshop #2: Data Analysis Pipeline End ---
}