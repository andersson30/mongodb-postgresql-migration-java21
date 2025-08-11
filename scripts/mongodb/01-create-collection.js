// MongoDB Script - Crear colección clientes y insertar datos de prueba
// Ejecutar con: mongo mongodb://localhost:27017/techtest scripts/mongodb/01-create-collection.js
// Usar la base de datos techtest
db = db.getSiblingDB("techtest");

// Crear la colección clientes (se crea automáticamente al insertar el primer documento)
// Pero podemos crear un índice para optimizar consultas por país
db.clientes.createIndex({ "direccion.pais": 1 });
db.clientes.createIndex({ "correo": 1 }, { unique: true });

// Insertar 10 documentos de prueba con datos variados
const clientesData = [
    {
        nombre: "Juan Pérez",
        correo: "juan.perez@email.com",
        direccion: {
            calle: "Calle Mayor 123",
            ciudad: "Madrid",
            pais: "España"
        }
    },
    {
        nombre: "María García",
        correo: "maria.garcia@email.com",
        direccion: {
            calle: "Av. Libertador 456",
            ciudad: "Buenos Aires",
            pais: "Argentina"
        }
    },
    {
        nombre: "Carlos Rodríguez",
        correo: "carlos.rodriguez@email.com",
        direccion: {
            calle: "Rua das Flores 789",
            ciudad: "São Paulo",
            pais: "Brasil"
        }
    },
    {
        nombre: "Ana Martínez",
        correo: "ana.martinez@email.com",
        direccion: {
            calle: "Paseo de la Reforma 321",
            ciudad: "Ciudad de México",
            pais: "México"
        }
    },
    {
        nombre: "Luis González",
        correo: "luis.gonzalez@email.com",
        direccion: {
            calle: "Carrera 7 #45-67",
            ciudad: "Bogotá",
            pais: "Colombia"
        }
    },
    {
        nombre: "Carmen López",
        correo: "carmen.lopez@email.com",
        direccion: {
            calle: "Gran Vía 890",
            ciudad: "Barcelona",
            pais: "España"
        }
    },
    {
        nombre: "Roberto Silva",
        correo: "roberto.silva@email.com",
        direccion: {
            calle: "Av. Paulista 1234",
            ciudad: "São Paulo",
            pais: "Brasil"
        }
    },
    {
        nombre: "Elena Fernández",
        correo: "elena.fernandez@email.com",
        direccion: {
            calle: "Calle Corrientes 567",
            ciudad: "Buenos Aires",
            pais: "Argentina"
        }
    },
    {
        nombre: "Miguel Torres",
        correo: "miguel.torres@email.com",
        direccion: {
            calle: "Av. Insurgentes 890",
            ciudad: "Guadalajara",
            pais: "México"
        }
    },
    {
        nombre: "Isabel Ruiz",
        correo: "isabel.ruiz@email.com",
        direccion: {
            calle: "Calle 72 #10-34",
            ciudad: "Medellín",
            pais: "Colombia"
        }
    }
];

// Insertar todos los documentos
const result = db.clientes.insertMany(clientesData);
print(`Insertados ${result.insertedIds.length} documentos de clientes`);

// Mostrar algunos documentos insertados
print("\n--- Documentos insertados ---");
db.clientes.find().limit(3).forEach(printjson);

print("\n--- Resumen de la colección ---");
print(`Total de documentos: ${db.clientes.countDocuments()}`);
print(`Países únicos: ${db.clientes.distinct("direccion.pais").length}`);
