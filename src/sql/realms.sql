-- MySQL dump 10.15  Distrib 10.0.14-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: test
-- ------------------------------------------------------
-- Server version	10.0.14-MariaDB-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Realm`
--

DROP TABLE IF EXISTS `Realm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Realm` (
  `RealmID` int(11) NOT NULL,
  `RName` varchar(30) NOT NULL,
  PRIMARY KEY (`RealmID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Realm`
--

LOCK TABLES `Realm` WRITE;
/*!40000 ALTER TABLE `Realm` DISABLE KEYS */;
INSERT INTO `Realm` VALUES (0,'Aegwynn'),(1,'Aerie Peak'),(2,'Agamaggan'),(3,'Aggramar'),(4,'Akama'),(5,'Alexstrasza'),(6,'Alleria'),(7,'Altar of Storms'),(8,'Alterac Mountains'),(9,'Aman\'Thul'),(10,'Andorhal'),(11,'Anetheron'),(12,'Antonidas'),(13,'Anub\'arak'),(14,'Anvilmar'),(15,'Arathor'),(16,'Archimonde'),(17,'Area 52'),(18,'Argent Dawn'),(19,'Arthas'),(20,'Arygos'),(21,'Auchindoun'),(22,'Azgalor'),(23,'Azjol-Nerub'),(24,'Azralon'),(25,'Azshara'),(26,'Azuremyst'),(27,'Baelgun'),(28,'Balnazzar'),(29,'Barthilas'),(30,'Black Dragonflight'),(31,'Blackhand'),(32,'Blackrock'),(33,'Blackwater Raiders'),(34,'Blackwing Lair'),(35,'Blade\'s Edge'),(36,'Bladefist'),(37,'Bleeding Hollow'),(38,'Blood Furnace'),(39,'Bloodhoof'),(40,'Bloodscalp'),(41,'Bonechewer'),(42,'Borean Tundra'),(43,'Boulderfist'),(44,'Bronzebeard'),(45,'Burning Blade'),(46,'Burning Legion'),(47,'Caelestrasz'),(48,'Cairne'),(49,'Cenarion Circle'),(50,'Cenarius'),(51,'Cho\'gall'),(52,'Chromaggus'),(53,'Coilfang'),(54,'Crushridge'),(55,'Daggerspine'),(56,'Dalaran'),(57,'Dalvengyr'),(58,'Dark Iron'),(59,'Darkspear'),(60,'Darrowmere'),(61,'Dath\'Remar'),(62,'Dawnbringer'),(63,'Deathwing'),(64,'Demon Soul'),(65,'Dentarg'),(66,'Destromath'),(67,'Dethecus'),(68,'Detheroc'),(69,'Doomhammer'),(70,'Draenor'),(71,'Dragonblight'),(72,'Dragonmaw'),(73,'Drak\'Tharon'),(74,'Drak\'thul'),(75,'Draka'),(76,'Drakkari'),(77,'Dreadmaul'),(78,'Drenden'),(79,'Dunemaul'),(80,'Durotan'),(81,'Duskwood'),(82,'Earthen Ring'),(83,'Echo Isles'),(84,'Eitrigg'),(85,'Eldre\'Thalas'),(86,'Elune'),(87,'Emerald Dream'),(88,'Eonar'),(89,'Eredar'),(90,'Executus'),(91,'Exodar'),(92,'Farstriders'),(93,'Feathermoon'),(94,'Fenris'),(95,'Firetree'),(96,'Fizzcrank'),(97,'Frostmane'),(98,'Frostmourne'),(99,'Frostwolf'),(100,'Galakrond'),(101,'Gallywix'),(102,'Garithos'),(103,'Garona'),(104,'Garrosh'),(105,'Ghostlands'),(106,'Gilneas'),(107,'Gnomeregan'),(108,'Goldrinn'),(109,'Gorefiend'),(110,'Gorgonnash'),(111,'Greymane'),(112,'Grizzly Hills'),(113,'Gul\'dan'),(114,'Gundrak'),(115,'Gurubashi'),(116,'Hakkar'),(117,'Haomarush'),(118,'Hellscream'),(119,'Hydraxis'),(120,'Hyjal'),(121,'Icecrown'),(122,'Illidan'),(123,'Jaedenar'),(124,'Jubei\'Thos'),(125,'Kael\'thas'),(126,'Kalecgos'),(127,'Kargath'),(128,'Kel\'Thuzad'),(129,'Khadgar'),(130,'Khaz Modan'),(131,'Khaz\'goroth'),(132,'Kil\'jaeden'),(133,'Kilrogg'),(134,'Kirin Tor'),(135,'Korgath'),(136,'Korialstrasz'),(137,'Kul Tiras'),(138,'Laughing Skull'),(139,'Lethon'),(140,'Lightbringer'),(141,'Lightning\'s Blade'),(142,'Lightninghoof'),(143,'Llane'),(144,'Lothar'),(145,'Madoran'),(146,'Maelstrom'),(147,'Magtheridon'),(148,'Maiev'),(149,'Mal\'Ganis'),(150,'Malfurion'),(151,'Malorne'),(152,'Malygos'),(153,'Mannoroth'),(154,'Medivh'),(155,'Misha'),(156,'Mok\'Nathal'),(157,'Moon Guard'),(158,'Moonrunner'),(159,'Mug\'thol'),(160,'Muradin'),(161,'Nagrand'),(162,'Nathrezim'),(163,'Nazgrel'),(164,'Nazjatar'),(165,'Nemesis'),(166,'Ner\'zhul'),(167,'Nesingwary'),(168,'Nordrassil'),(169,'Norgannon'),(170,'Onyxia'),(171,'Perenolde'),(172,'Proudmoore'),(173,'Quel\'Thalas'),(174,'Quel\'dorei'),(175,'Ragnaros'),(176,'Ravencrest'),(177,'Ravenholdt'),(178,'Rexxar'),(179,'Rivendare'),(180,'Runetotem'),(181,'Sargeras'),(182,'Saurfang'),(183,'Scarlet Crusade'),(184,'Scilla'),(185,'Sen\'jin'),(186,'Sentinels'),(187,'Shadow Council'),(188,'Shadowmoon'),(189,'Shadowsong'),(190,'Shandris'),(191,'Shattered Halls'),(192,'Shattered Hand'),(193,'Shu\'halo'),(194,'Silver Hand'),(195,'Silvermoon'),(196,'Sisters of Elune'),(197,'Skullcrusher'),(198,'Skywall'),(199,'Smolderthorn'),(200,'Spinebreaker'),(201,'Spirestone'),(202,'Staghelm'),(203,'Steamwheedle Cartel'),(204,'Stonemaul'),(205,'Stormrage'),(206,'Stormreaver'),(207,'Stormscale'),(208,'Suramar'),(209,'Tanaris'),(210,'Terenas'),(211,'Terokkar'),(212,'Thaurissan'),(213,'The Forgotten Coast'),(214,'The Scryers'),(215,'The Underbog'),(216,'The Venture Co'),(217,'Thorium Brotherhood'),(218,'Thrall'),(219,'Thunderhorn'),(220,'Thunderlord'),(221,'Tichondrius'),(222,'Tol Barad'),(223,'Tortheldrin'),(224,'Trollbane'),(225,'Turalyon'),(226,'Twisting Nether'),(227,'Uldaman'),(228,'Uldum'),(229,'Undermine'),(230,'Ursin'),(231,'Uther'),(232,'Vashj'),(233,'Vek\'nilash'),(234,'Velen'),(235,'Warsong'),(236,'Whisperwind'),(237,'Wildhammer'),(238,'Windrunner'),(239,'Winterhoof'),(240,'Wyrmrest Accord'),(241,'Ysera'),(242,'Ysondre'),(243,'Zangarmarsh'),(244,'Zul\'jin'),(245,'Zuluhed');
/*!40000 ALTER TABLE `Realm` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2014-12-06 16:46:52
