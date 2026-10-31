-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: fle_presence
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `creneau`
--

DROP TABLE IF EXISTS `creneau`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `creneau` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `groupe_id` bigint NOT NULL,
  `jour_semaine` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `heure_debut` time NOT NULL,
  `heure_fin` time NOT NULL,
  `salle` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `fk_creneau_groupe` (`groupe_id`),
  CONSTRAINT `fk_creneau_groupe` FOREIGN KEY (`groupe_id`) REFERENCES `groupe` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `creneau`
--

LOCK TABLES `creneau` WRITE;
/*!40000 ALTER TABLE `creneau` DISABLE KEYS */;
INSERT INTO `creneau` VALUES (1,1,'TUESDAY','09:30:00','11:00:00',NULL,1);
/*!40000 ALTER TABLE `creneau` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','schema','SQL','V1__schema.sql',258565415,'fle','2026-10-31 05:25:12',663,1),(2,'2','journal notifications','SQL','V2__journal_notifications.sql',1872512950,'fle','2026-10-31 09:17:16',427,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `groupe`
--

DROP TABLE IF EXISTS `groupe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `groupe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `niveau` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unite_id` bigint NOT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `fk_groupe_unite` (`unite_id`),
  CONSTRAINT `fk_groupe_unite` FOREIGN KEY (`unite_id`) REFERENCES `unite_locale` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `groupe`
--

LOCK TABLES `groupe` WRITE;
/*!40000 ALTER TABLE `groupe` DISABLE KEYS */;
INSERT INTO `groupe` VALUES (1,'groupe_du_mardi',NULL,2,1);
/*!40000 ALTER TABLE `groupe` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `journal`
--

DROP TABLE IF EXISTS `journal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `journal` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_action` datetime NOT NULL,
  `auteur` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unite_id` bigint DEFAULT NULL,
  `action` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `details` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_journal_unite` (`unite_id`),
  KEY `idx_journal_date` (`date_action`),
  CONSTRAINT `fk_journal_unite` FOREIGN KEY (`unite_id`) REFERENCES `unite_locale` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `journal`
--

LOCK TABLES `journal` WRITE;
/*!40000 ALTER TABLE `journal` DISABLE KEYS */;
/*!40000 ALTER TABLE `journal` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parametre`
--

DROP TABLE IF EXISTS `parametre`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parametre` (
  `cle` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `valeur` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`cle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parametre`
--

LOCK TABLES `parametre` WRITE;
/*!40000 ALTER TABLE `parametre` DISABLE KEYS */;
INSERT INTO `parametre` VALUES ('vapid.private','F1ZcwRSoSm3vS8-reSA1OBtd2g2Mw5Uo_lDtj-naI1o'),('vapid.public','BDgegR9cQshe_WYVSCnBBLXWgmUeBXDM5-PYmZLWnzfKvEcz3_cedhRRROmJyZp_c_Ns13nyv4M8q81BKVmNa5M');
/*!40000 ALTER TABLE `parametre` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `presence`
--

DROP TABLE IF EXISTS `presence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `presence` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `seance_id` bigint NOT NULL,
  `nb_benevoles` int NOT NULL,
  `nb_apprenants` int NOT NULL,
  `commentaire` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `saisi_par` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `saisi_le` datetime NOT NULL,
  `modifie_par` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `modifie_le` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_presence_seance` (`seance_id`),
  CONSTRAINT `fk_presence_seance` FOREIGN KEY (`seance_id`) REFERENCES `seance` (`id`),
  CONSTRAINT `ck_presence_nombres` CHECK (((`nb_benevoles` >= 0) and (`nb_apprenants` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `presence`
--

LOCK TABLES `presence` WRITE;
/*!40000 ALTER TABLE `presence` DISABLE KEYS */;
/*!40000 ALTER TABLE `presence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `push_abonnement`
--

DROP TABLE IF EXISTS `push_abonnement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `push_abonnement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `unite_id` bigint NOT NULL,
  `groupe_id` bigint DEFAULT NULL,
  `endpoint` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `endpoint_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `p256dh` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `auth` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cree_le` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_push_endpoint` (`endpoint_hash`),
  KEY `fk_push_unite` (`unite_id`),
  KEY `fk_push_groupe` (`groupe_id`),
  CONSTRAINT `fk_push_groupe` FOREIGN KEY (`groupe_id`) REFERENCES `groupe` (`id`),
  CONSTRAINT `fk_push_unite` FOREIGN KEY (`unite_id`) REFERENCES `unite_locale` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `push_abonnement`
--

LOCK TABLES `push_abonnement` WRITE;
/*!40000 ALTER TABLE `push_abonnement` DISABLE KEYS */;
/*!40000 ALTER TABLE `push_abonnement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `seance`
--

DROP TABLE IF EXISTS `seance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `groupe_id` bigint NOT NULL,
  `creneau_id` bigint DEFAULT NULL,
  `date_seance` date NOT NULL,
  `heure_debut` time NOT NULL,
  `heure_fin` time NOT NULL,
  `salle` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `statut` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PREVUE',
  `motif` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reportee_vers_id` bigint DEFAULT NULL,
  `modifiee_le` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seance_groupe_date_heure` (`groupe_id`,`date_seance`,`heure_debut`),
  KEY `fk_seance_creneau` (`creneau_id`),
  KEY `fk_seance_report` (`reportee_vers_id`),
  KEY `idx_seance_date` (`date_seance`),
  CONSTRAINT `fk_seance_creneau` FOREIGN KEY (`creneau_id`) REFERENCES `creneau` (`id`),
  CONSTRAINT `fk_seance_groupe` FOREIGN KEY (`groupe_id`) REFERENCES `groupe` (`id`),
  CONSTRAINT `fk_seance_report` FOREIGN KEY (`reportee_vers_id`) REFERENCES `seance` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `seance`
--

LOCK TABLES `seance` WRITE;
/*!40000 ALTER TABLE `seance` DISABLE KEYS */;
INSERT INTO `seance` VALUES (1,1,1,'2026-11-03','09:30:00','11:00:00',NULL,'PREVUE',NULL,NULL,NULL),(2,1,1,'2026-11-10','09:30:00','11:00:00',NULL,'ANNULEE',NULL,NULL,'2026-10-31 06:40:50'),(3,1,1,'2026-11-17','09:30:00','11:00:00',NULL,'PREVUE',NULL,NULL,NULL),(4,1,1,'2026-11-24','09:30:00','11:00:00',NULL,'PREVUE',NULL,NULL,NULL),(5,1,1,'2026-12-01','09:30:00','11:00:00',NULL,'PREVUE',NULL,NULL,NULL),(6,1,1,'2026-12-08','09:30:00','11:00:00',NULL,'PREVUE',NULL,NULL,NULL),(7,1,1,'2026-12-15','09:30:00','11:00:00',NULL,'PREVUE',NULL,NULL,NULL),(8,1,1,'2026-12-22','09:30:00','11:00:00',NULL,'PREVUE',NULL,NULL,NULL);
/*!40000 ALTER TABLE `seance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `unite_locale`
--

DROP TABLE IF EXISTS `unite_locale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `unite_locale` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `slug` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `adresse` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unite_nom` (`nom`),
  UNIQUE KEY `uk_unite_slug` (`slug`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `unite_locale`
--

LOCK TABLES `unite_locale` WRITE;
/*!40000 ALTER TABLE `unite_locale` DISABLE KEYS */;
INSERT INTO `unite_locale` VALUES (1,'Chatou','chatou',NULL,1),(2,'Sartrouville','sartrouville',NULL,1);
/*!40000 ALTER TABLE `unite_locale` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `utilisateur`
--

DROP TABLE IF EXISTS `utilisateur`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `utilisateur` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nom_affiche` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unite_id` bigint DEFAULT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_utilisateur_username` (`username`),
  KEY `fk_utilisateur_unite` (`unite_id`),
  CONSTRAINT `fk_utilisateur_unite` FOREIGN KEY (`unite_id`) REFERENCES `unite_locale` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `utilisateur`
--

LOCK TABLES `utilisateur` WRITE;
/*!40000 ALTER TABLE `utilisateur` DISABLE KEYS */;
INSERT INTO `utilisateur` VALUES (1,'admin','$2a$10$Dehvm0NlNBFbke4mJZ/9cewyQBTCqUW1KmOXOM0pfJlusB/m7gR9S','Administrateur','ADMIN',NULL,1),(2,'responsable_sartrouville','$2a$10$oyQoQsWYs2asJEeNQa6jJ.DRWMLJelmceWDBiUfAGe.S4mh1YUFhO','Fanny','RESPONSABLE',2,1),(3,'roland_sartrouville','$2a$10$8Jjnabw.SIm9LR.Et3Lp0uC.Zy7XA79dyLBG3PpGqGiv0x9mVCbN2','Roland','BENEVOLE',2,1);
/*!40000 ALTER TABLE `utilisateur` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-31 16:25:05
