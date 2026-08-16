package org.example

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

// --- 1. Data Modeling ---
// ใช้ @Serializable เพื่อให้ Ktor แปลงเป็น JSON ได้

@Serializable
data class Task(val id: Int, val content: String, val isDone: Boolean)

// data class สำหรับรับ request ตอนสร้าง Task ใหม่ (ยังไม่มี ID)
@Serializable
data class TaskRequest(val content: String, val isDone: Boolean)

// --- 2. Data Layer (In-Memory) ---
// จำลองฐานข้อมูลในหน่วยความจำด้วย mutableListOf<Task>
object TaskRepository {
    private val tasks = mutableListOf<Task>()
    private var nextId = 1

    // ดึงข้อมูล tasks ทั้งหมด
    fun getAll(): List<Task> = tasks.toList()

    // ดึงข้อมูล tasks by id
    fun getById(id: Int): Task? = tasks.find { it.id == id }

    // เพิ่มข้อมูล task เข้าไป
    fun add(task: TaskRequest): Task {
        val newTask = Task(id = nextId++, content = task.content, isDone = task.isDone)
        tasks.add(newTask)
        return newTask
    }

    // update ข้อมูล task ตาม id
    fun update(id: Int, updatedTask: TaskRequest): Task? {
        val index = tasks.indexOfFirst { it.id == id }
        if (index == -1) return null
        val updated = Task(id = id, content = updatedTask.content, isDone = updatedTask.isDone)
        tasks[index] = updated
        return updated
    }

    // ลบข้อมูล task จาก id
    fun delete(id: Int): Boolean {
        return tasks.removeIf { it.id == id }
    }
}

// --- 3. Main Application ---
fun main() {
    embeddedServer(Netty, port = 8080) {
        // ติดตั้ง plugin ContentNegotiation เพื่อรองรับ JSON
        install(ContentNegotiation) {
            json()
        }

        routing {
            // GET /tasks — คืนค่า task ทั้งหมด (200 OK)
            get("/tasks") {
                call.respond(HttpStatusCode.OK, TaskRepository.getAll())
            }

            // GET /tasks/{id} — คืนค่า task ตาม id หรือ 404 Not Found
            get("/tasks/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID")

                val task = TaskRepository.getById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "Task not found")

                call.respond(HttpStatusCode.OK, task)
            }

            // POST /tasks — รับ TaskRequest, เพิ่มลง repository, ตอบ 201 Created
            post("/tasks") {
                val request = call.receive<TaskRequest>()
                val newTask = TaskRepository.add(request)
                call.respond(HttpStatusCode.Created, newTask)
            }

            // PUT /tasks/{id} — รับ id และ TaskRequest, อัปเดตข้อมูล, ตอบ 200 OK
            put("/tasks/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")

                val request = call.receive<TaskRequest>()
                val updated = TaskRepository.update(id, request)
                    ?: return@put call.respond(HttpStatusCode.NotFound, "Task not found")

                call.respond(HttpStatusCode.OK, updated)
            }

            // DELETE /tasks/{id} — ลบ task, ตอบ 204 No Content
            delete("/tasks/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID")

                val deleted = TaskRepository.delete(id)
                if (!deleted) {
                    return@delete call.respond(HttpStatusCode.NotFound, "Task not found")
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }.start(wait = true)
}
