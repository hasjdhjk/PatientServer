# Remote Patient Monitoring System

A full-stack **Remote Patient Monitoring (RPM)** application built using a **Java client–server architecture**.  
The system enables clinicians to manage patients, monitor simulated vital signs in real time, and review historical records through a desktop interface.

---

## Table of Contents
- [Overview](#overview)
- [Key Features](#key-features)
- [Technologies Used](#technologies-used)
- [Deployment](#deployment)
- [Configuration](#configuration)
- [Usage](#usage)
- [Design Notes](#design-notes)
- [Author](#author)

---

## Overview

This project was developed as part of a **Software Engineering for Bioengineers** course.  
It demonstrates full-stack system design, RESTful communication, database persistence, and cloud deployment using simulated biomedical data.

The system consists of:
- A **Java desktop client** for clinicians
- A **Java Servlet backend server**
- A **PostgreSQL database**
- Deployment on **Tsuru (IMPAAS)**


---

## Key Features

### Client (Clinician Application)
- Doctor authentication (login / register / reset password)
- Patient creation, discharge, and search
- Live monitoring of:
  - Heart rate (ECG simulation)
  - Respiratory rate
  - SpO₂
  - Temperature
  - Blood pressure
- Real-time waveform visualisation
- Digital Twin patient view
- Historical vital records table
- PDF and JSON export of patient data
- Light / dark theme support

### Server (Backend API)
- RESTful API using Java Servlets
- Secure database access via DAO layer
- PostgreSQL persistence
- Patient–doctor data isolation
- Cloud-safe configuration using environment variables

---

## Technologies Used

- **Java 17**
- **Swing / JavaFX**
- **Java Servlets**
- **PostgreSQL**
- **JDBC**
- **Gson**
- **Maven**
- **Tsuru (IMPAAS)**

---

## Deployment

Both the **client** and **server** are deployed on **Tsuru**.

- Server connects to PostgreSQL via environment variables
- Client communicates with server using a configurable base URL

---

## Configuration

### Required Environment Variables (Server)

- PGHOST
- PGPASSWORD
- PGPORT
- PGUSER
- TSURU_SERVICES
- TSURU_APPNAME
- TSURU_APPDIR
- PGDATABASE

---

## Usage



---

## Design Notes

- Client–server separation ensures scalability and maintainability
- DAO pattern isolates database logic
- Vital signs are generated using parameterised physiological models
- Shared in-memory caching is used on the client for responsive UI updates
- System designed to be extensible to real sensor input in future work

---

## Author

- **Zihao Tan**  
- **Ze Zhou**
- **JianKun Ren**
- **Xuan Li Feng**
- **David Wong**

MEng Biomedical Engineering  
Imperial College London
