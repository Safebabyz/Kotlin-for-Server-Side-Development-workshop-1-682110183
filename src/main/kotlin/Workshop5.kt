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

// ============================================================
// Workshop 5: Recipe Book API
// ฝึกฝน: One-to-Many Relationship, Complex Query, Nested Data
// ============================================================

// --- 1. Data Modeling ---

// Ingredient ที่อยู่ภายใน Recipe (nested data)
@Serializable
data class Ingredient(
    val id: Int,
    val name: String,
    val quantity: Double,
    val unit: String
)

// Recipe พร้อม Ingredients แบบ Nested
@Serializable
data class Recipe(
    val id: Int,
    val name: String,
    val instructions: String,
    val ingredients: List<Ingredient> = emptyList()
)

// Request body สำหรับสร้าง/อัปเดต Recipe (ยังไม่มี ID)
@Serializable
data class RecipeRequest(
    val name: String,
    val instructions: String
)

// Request body สำหรับสร้าง/อัปเดต Ingredient (ยังไม่มี ID)
@Serializable
data class IngredientRequest(
    val name: String,
    val quantity: Double,
    val unit: String
)

// --- 2. Data Layer (In-Memory) ---
// จำลองฐานข้อมูลด้วย mutableListOf แสดงความสัมพันธ์แบบ One-to-Many
object RecipeRepository {

    // เก็บ recipes และ ingredients แยกกัน โดยใช้ recipeId เชื่อม (เหมือน Foreign Key)
    private val recipes = mutableListOf<Recipe>()
    private val ingredients = mutableListOf<Ingredient>()

    // Map เก็บว่า ingredient แต่ละตัวอยู่ใน recipe ไหน (ingredientId -> recipeId)
    private val ingredientToRecipe = mutableMapOf<Int, Int>()

    private var nextRecipeId = 1
    private var nextIngredientId = 1

    // ----- Recipe Operations -----

    // ดึง recipe ทั้งหมด พร้อม ingredients ของแต่ละ recipe (Nested Data)
    fun getAllRecipes(): List<Recipe> {
        return recipes.map { recipe ->
            recipe.copy(ingredients = getIngredientsForRecipe(recipe.id))
        }
    }

    // ดึง recipe ตาม id พร้อม ingredients (Nested Data)
    fun getRecipeById(id: Int): Recipe? {
        val recipe = recipes.find { it.id == id } ?: return null
        return recipe.copy(ingredients = getIngredientsForRecipe(id))
    }

    // เพิ่ม recipe ใหม่
    fun addRecipe(request: RecipeRequest): Recipe {
        val newRecipe = Recipe(
            id = nextRecipeId++,
            name = request.name,
            instructions = request.instructions
        )
        recipes.add(newRecipe)
        return newRecipe
    }

    // อัปเดต recipe ตาม id
    fun updateRecipe(id: Int, request: RecipeRequest): Recipe? {
        val index = recipes.indexOfFirst { it.id == id }
        if (index == -1) return null
        val updated = Recipe(id = id, name = request.name, instructions = request.instructions)
        recipes[index] = updated
        return updated.copy(ingredients = getIngredientsForRecipe(id))
    }

    // ลบ recipe ตาม id (พร้อมลบ ingredients ทั้งหมดที่อยู่ใน recipe นั้นด้วย)
    fun deleteRecipe(id: Int): Boolean {
        val removed = recipes.removeIf { it.id == id }
        if (removed) {
            // ลบ ingredients ที่เชื่อมกับ recipe นี้ทั้งหมด (Cascade Delete)
            val ingredientIds = ingredientToRecipe.filter { it.value == id }.keys.toList()
            ingredientIds.forEach { ingredientId ->
                ingredients.removeIf { it.id == ingredientId }
                ingredientToRecipe.remove(ingredientId)
            }
        }
        return removed
    }

    // ----- Ingredient Operations (One-to-Many) -----

    // ดึง ingredients ทั้งหมดของ recipe ที่ระบุ
    fun getIngredientsForRecipe(recipeId: Int): List<Ingredient> {
        val ingredientIds = ingredientToRecipe.filter { it.value == recipeId }.keys
        return ingredients.filter { it.id in ingredientIds }
    }

    // ดึง ingredient ตาม id (ภายใน recipe ที่ระบุ)
    fun getIngredientById(recipeId: Int, ingredientId: Int): Ingredient? {
        if (ingredientToRecipe[ingredientId] != recipeId) return null
        return ingredients.find { it.id == ingredientId }
    }

    // เพิ่ม ingredient เข้าไปใน recipe ที่ระบุ
    fun addIngredient(recipeId: Int, request: IngredientRequest): Ingredient? {
        // ตรวจสอบว่า recipe มีอยู่จริง
        if (recipes.none { it.id == recipeId }) return null
        val newIngredient = Ingredient(
            id = nextIngredientId++,
            name = request.name,
            quantity = request.quantity,
            unit = request.unit
        )
        ingredients.add(newIngredient)
        ingredientToRecipe[newIngredient.id] = recipeId  // เชื่อม ingredient กับ recipe
        return newIngredient
    }

    // อัปเดต ingredient ตาม id (ภายใน recipe ที่ระบุ)
    fun updateIngredient(recipeId: Int, ingredientId: Int, request: IngredientRequest): Ingredient? {
        if (ingredientToRecipe[ingredientId] != recipeId) return null
        val index = ingredients.indexOfFirst { it.id == ingredientId }
        if (index == -1) return null
        val updated = Ingredient(
            id = ingredientId,
            name = request.name,
            quantity = request.quantity,
            unit = request.unit
        )
        ingredients[index] = updated
        return updated
    }

    // ลบ ingredient ตาม id (ภายใน recipe ที่ระบุ)
    fun deleteIngredient(recipeId: Int, ingredientId: Int): Boolean {
        if (ingredientToRecipe[ingredientId] != recipeId) return false
        val removed = ingredients.removeIf { it.id == ingredientId }
        if (removed) ingredientToRecipe.remove(ingredientId)
        return removed
    }

    // ----- Search Operation (Complex Query) -----

    // ค้นหา recipes ที่มีส่วนผสมที่ชื่อตรงกับ keyword (case-insensitive)
    // เช่น GET /recipes/search?ingredient=chicken
    fun searchByIngredient(ingredientName: String): List<Recipe> {
        // 1. หา ingredientIds ที่ชื่อตรงกับ keyword
        val matchingIngredientIds = ingredients
            .filter { it.name.contains(ingredientName, ignoreCase = true) }
            .map { it.id }

        // 2. หา recipeIds จาก ingredientIds ที่ match (ลบ duplicate ด้วย toSet())
        val matchingRecipeIds = matchingIngredientIds
            .mapNotNull { ingredientToRecipe[it] }
            .toSet()

        // 3. ดึง recipes ที่ตรงกัน พร้อม nested ingredients
        return matchingRecipeIds
            .mapNotNull { getRecipeById(it) }
    }
}

// --- 3. Main Application ---
fun main() {
    embeddedServer(Netty, port = 8080) {
        // ติดตั้ง ContentNegotiation plugin สำหรับ JSON
        install(ContentNegotiation) {
            json()
        }

        routing {

            // ================================================================
            // RECIPE ENDPOINTS (CRUD)
            // ================================================================

            // GET /recipes — ดึง recipes ทั้งหมด พร้อม nested ingredients
            get("/recipes") {
                call.respond(HttpStatusCode.OK, RecipeRepository.getAllRecipes())
            }

            // GET /recipes/search?ingredient={name} — ค้นหา recipe จากชื่อส่วนผสม
            // ต้องวางไว้ก่อน /recipes/{id} เพื่อไม่ให้ "search" ถูกตีความเป็น id
            get("/recipes/search") {
                val ingredientName = call.request.queryParameters["ingredient"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Query parameter 'ingredient' is required"
                    )

                val results = RecipeRepository.searchByIngredient(ingredientName)
                call.respond(HttpStatusCode.OK, results)
            }

            // GET /recipes/{id} — ดึง recipe ตาม id พร้อม nested ingredients
            get("/recipes/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid recipe ID")

                val recipe = RecipeRepository.getRecipeById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "Recipe not found")

                call.respond(HttpStatusCode.OK, recipe)
            }

            // POST /recipes — สร้าง recipe ใหม่
            post("/recipes") {
                val request = call.receive<RecipeRequest>()
                val newRecipe = RecipeRepository.addRecipe(request)
                call.respond(HttpStatusCode.Created, newRecipe)
            }

            // PUT /recipes/{id} — อัปเดต recipe ตาม id
            put("/recipes/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid recipe ID")

                val request = call.receive<RecipeRequest>()
                val updated = RecipeRepository.updateRecipe(id, request)
                    ?: return@put call.respond(HttpStatusCode.NotFound, "Recipe not found")

                call.respond(HttpStatusCode.OK, updated)
            }

            // DELETE /recipes/{id} — ลบ recipe (และ ingredients ทั้งหมดในนั้น)
            delete("/recipes/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid recipe ID")

                val deleted = RecipeRepository.deleteRecipe(id)
                if (!deleted) {
                    return@delete call.respond(HttpStatusCode.NotFound, "Recipe not found")
                }

                call.respond(HttpStatusCode.NoContent)
            }

            // ================================================================
            // INGREDIENT ENDPOINTS (Nested CRUD ภายใต้ Recipe)
            // ================================================================

            // GET /recipes/{recipeId}/ingredients — ดึง ingredients ทั้งหมดของ recipe
            get("/recipes/{recipeId}/ingredients") {
                val recipeId = call.parameters["recipeId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid recipe ID")

                // ตรวจสอบว่า recipe มีอยู่จริง
                RecipeRepository.getRecipeById(recipeId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "Recipe not found")

                call.respond(HttpStatusCode.OK, RecipeRepository.getIngredientsForRecipe(recipeId))
            }

            // GET /recipes/{recipeId}/ingredients/{id} — ดึง ingredient ตาม id
            get("/recipes/{recipeId}/ingredients/{id}") {
                val recipeId = call.parameters["recipeId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid recipe ID")
                val ingredientId = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ingredient ID")

                val ingredient = RecipeRepository.getIngredientById(recipeId, ingredientId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "Ingredient not found in this recipe")

                call.respond(HttpStatusCode.OK, ingredient)
            }

            // POST /recipes/{recipeId}/ingredients — เพิ่ม ingredient เข้า recipe
            post("/recipes/{recipeId}/ingredients") {
                val recipeId = call.parameters["recipeId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid recipe ID")

                val request = call.receive<IngredientRequest>()
                val newIngredient = RecipeRepository.addIngredient(recipeId, request)
                    ?: return@post call.respond(HttpStatusCode.NotFound, "Recipe not found")

                call.respond(HttpStatusCode.Created, newIngredient)
            }

            // PUT /recipes/{recipeId}/ingredients/{id} — อัปเดต ingredient ตาม id
            put("/recipes/{recipeId}/ingredients/{id}") {
                val recipeId = call.parameters["recipeId"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid recipe ID")
                val ingredientId = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ingredient ID")

                val request = call.receive<IngredientRequest>()
                val updated = RecipeRepository.updateIngredient(recipeId, ingredientId, request)
                    ?: return@put call.respond(HttpStatusCode.NotFound, "Ingredient not found in this recipe")

                call.respond(HttpStatusCode.OK, updated)
            }

            // DELETE /recipes/{recipeId}/ingredients/{id} — ลบ ingredient ออกจาก recipe
            delete("/recipes/{recipeId}/ingredients/{id}") {
                val recipeId = call.parameters["recipeId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid recipe ID")
                val ingredientId = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ingredient ID")

                val deleted = RecipeRepository.deleteIngredient(recipeId, ingredientId)
                if (!deleted) {
                    return@delete call.respond(HttpStatusCode.NotFound, "Ingredient not found in this recipe")
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }.start(wait = true)
}
