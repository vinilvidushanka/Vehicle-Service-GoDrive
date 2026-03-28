# 🚗 GoDrive - Vehicle Management Service

## 1. Overview
The **Vehicle Service** is a core business microservice of the **GoDrive** ecosystem. It handles all operations related to vehicle inventory management, including adding, updating, and retrieving vehicle details.

## 2. Key Features
* **Inventory Management:** CRUD operations for vehicle records (Make, Model, Year, etc.).
* **Cloud Storage Integration:** Uploads and retrieves vehicle images using **Google Cloud Storage Buckets**.
* **Service Discovery:** Automatically registers with the **Eureka Service Registry**.
* **Externalized Config:** Fetches runtime configurations from the **Config Server**.

---

## 3. Technology Stack
* **Language:** Java 25 
* **Framework:** Spring Boot & Spring Data JPA 
* **Database (Relational):** **Cloud SQL (MySQL)** for structured vehicle data 
* **Cloud Integration:** Google Cloud Storage (GCP SDK) 

---

## 4. API Endpoints (via API Gateway)
All requests should be routed through the API Gateway.
* `GET http://34.93.114.63:8082/api/v1/vehicles` - Retrieve all vehicles.
* `GET http://34.93.114.63:8082/api/v1/vehicles/{id}` - Get specific vehicle details.
* `POST http://34.93.114.63:8082/api/v1/vehicles/save-with-image` - Register a new vehicle.
* `PUT http://34.93.114.63:8082/api/v1/vehicles/{id}` - Update vehicle information.
* `DELETE http://34.93.114.63:8082/api/v1/vehicles/{id}` - Remove a vehicle from the system.

---

## 5. Reliability & Deployment
* **Process Management:** Managed by **PM2** for automatic restarts on failure.
* **High Availability:** Deployed in **Managed Instance Groups (MIGs)** with auto-scaling enabled to handle varying traffic.
