# Patient Monitoring Server (Backend)

This repository contains the **backend server** for the Patient Monitoring System.  
It provides REST APIs for **doctor authentication**, **patient management**, and **data persistence**, and is deployed on the **Tsuru cloud platform** with a **PostgreSQL database**.

<br>

## Overview

The backend is responsible for:

- Doctor registration and login
- Secure storage of doctor accounts in a server-side database
- Patient data management per authenticated doctor
- Serving data to the Java Swing client via RESTful APIs
- Database schema creation and migration on deployment

The server is implemented using **Java Servlets**, **JDBC**, and **PostgreSQL**, and follows a layered architecture (Servlet → DAO → Database).

<br>

## Technology Stack

- Java (Servlets)
- PostgreSQL
- JDBC
- Gson (JSON serialization)
- Tsuru Cloud Platform
- Environment-variable based configuration

<br>

## Architecture

The backend follows a standard layered design:

- **Servlet Layer**
  - Handles HTTP requests and responses
  - Performs validation and JSON parsing
  - Exposes REST API endpoints

- **DAO Layer**
  - Encapsulates all database access logic
  - Uses prepared statements to prevent SQL injection
  - Provides clear separation between business logic and persistence

- **Database Layer**
  - PostgreSQL database hosted on Tsuru
  - Automatic schema initialization and migration on startup

<br>

## Database Schema

### Doctors Table

Stores clinician account information.

| Column               | Type             | Description                     |
|----------------------|------------------|---------------------------------|
| id                   | SERIAL           | Primary key                     |
| email                | TEXT (UNIQUE)    | Doctor login email              |
| given_name           | TEXT             | First name                      |
| family_name          | TEXT             | Last name                       |
| password_hash        | TEXT             | Hashed password                 |
| verified             | BOOLEAN          | Account verification status     |
| verification_token   | TEXT             | Email verification token        |
| reset_token          | TEXT             | Password reset token            |
| reset_token_expires  | TIMESTAMP        | Reset token expiry              |

<br>

### Patients Table

Stores patients associated with a doctor account.

| Column          | Type    | Description                               |
|-----------------|---------|-------------------------------------------|
| id              | SERIAL  | Primary key                               |
| doctor          | TEXT    | Owning doctor email                      |
| given_name      | TEXT    | Patient first name                       |
| family_name     | TEXT    | Patient last name                        |
| gender          | TEXT    | Gender                                   |
| age             | INT     | Age                                      |
| blood_pressure  | TEXT    | Blood pressure (e.g. 120/80)             |

<br>

## API Endpoints

The backend exposes RESTful API endpoints for authentication and patient management.
All endpoints consume and return **JSON** and follow standard HTTP status conventions.

<br>

### Authentication

Authentication endpoints handle doctor registration, login, and account verification.
Passwords are **never stored in plaintext** and are securely hashed before persistence.

<br>

#### Register Doctor

`curl -sS -X POST "https://bioeng-bbb-app.impaas.uk/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"[YOUREMAIL]","password":"[YOURPASSWORD]","givenName":"[GIVENNAME]","familyName":"[FAMILYNAME]"}'`

Registers a new doctor account in the system.

#### Empty demo patient database
`curl -X POST "https://bioeng-bbb-app.impaas.uk/api/patients?action=clearDemo&doctor=demo"`
