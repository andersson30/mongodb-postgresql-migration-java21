// MongoDB Script - Consultas y operaciones
// Ejecutar con: mongo mongodb://localhost:27017/techtest scripts/mongodb/02-queries.js

db = db.getSiblingDB("techtest");

print("=== CONSULTAS MONGODB ===\n");

// 1. Listar clientes filtrando por país
print("1. Clientes de España:");
db.clientes.find(
    { "direccion.pais": "España" },
    { nombre: 1, correo: 1, "direccion.ciudad": 1 }
).forEach(printjson);

print("\n2. Clientes de Brasil:");
db.clientes.find(
    { "direccion.pais": "Brasil" },
    { nombre: 1, correo: 1, "direccion.ciudad": 1 }
).forEach(printjson);

print("\n3. Clientes de Argentina:");
db.clientes.find(
    { "direccion.pais": "Argentina" },
    { nombre: 1, correo: 1, "direccion.ciudad": 1 }
).forEach(printjson);

// 2. Actualizar el correo de un cliente específico
print("\n=== ACTUALIZACIÓN DE CORREO ===");

// Buscar un cliente específico antes de actualizar
const clienteAntes = db.clientes.findOne({ nombre: "Juan Pérez" });
print("Cliente antes de la actualización:");
printjson(clienteAntes);

// Actualizar el correo
const updateResult = db.clientes.updateOne(
    { nombre: "Juan Pérez" },
    { $set: { correo: "juan.perez.nuevo@email.com" } }
);

print(`\nDocumentos actualizados: ${updateResult.modifiedCount}`);

// Verificar la actualización
const clienteDespues = db.clientes.findOne({ nombre: "Juan Pérez" });
print("Cliente después de la actualización:");
printjson(clienteDespues);

// 3. Consultas adicionales útiles para el proyecto
print("\n=== CONSULTAS ADICIONALES ===");

print("\n3.1 Contar clientes por país:");
db.clientes.aggregate([
    {
        $group: {
            _id: "$direccion.pais",
            total: { $sum: 1 }
        }
    },
    { $sort: { total: -1 } }
]).forEach(printjson);

print("\n3.2 Listar todas las ciudades únicas:");
const ciudades = db.clientes.distinct("direccion.ciudad");
print(`Ciudades: ${ciudades.join(", ")}`);

print("\n3.3 Buscar clientes por ciudad:");
db.clientes.find(
    { "direccion.ciudad": "São Paulo" },
    { nombre: 1, correo: 1 }
).forEach(printjson);

print("\n=== ESTADÍSTICAS FINALES ===");
print(`Total de clientes: ${db.clientes.countDocuments()}`);
print(`Países únicos: ${db.clientes.distinct("direccion.pais").length}`);
print(`Ciudades únicas: ${db.clientes.distinct("direccion.ciudad").length}`);
