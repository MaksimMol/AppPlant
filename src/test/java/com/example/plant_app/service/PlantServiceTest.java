package com.example.plant_app.service;

import com.example.plant_app.dao.PlantDAO;
import com.example.plant_app.model.Plant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlantServiceTest {

    @Mock
    private PlantDAO mockDao;

    @InjectMocks
    private PlantService plantService;

    private Plant plant1;
    private Plant plant2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Пример растений
        plant1 = new Plant("Роза", "декоративно-цветущие", LocalDate.now().minusDays(1),
                1.0, Arrays.asList("желтые листья"), "Активно");

        plant2 = new Plant("Кактус", "суккуленты", LocalDate.now().minusDays(40),
                0.5, new ArrayList<>(), "Неактивно");
    }

    @Test
    void testAddPlant_Success() {
        plantService.addDAO(mockDao);

        plantService.addPlant(plant1);

        verify(mockDao, times(1)).addPlant(plant1);
        assertEquals("декоративно-цветущие", plant1.getType());
        assertNotNull(plant1.getNextWatering());
    }

    @Test
    void testAddPlant_InvalidName() {
        Plant invalid = new Plant("Неизвестное", "декоративно-цветущие", LocalDate.now(), 1.0, new ArrayList<>(), "Активно");

        Exception ex = assertThrows(IllegalArgumentException.class, () -> plantService.addPlant(invalid));
        assertTrue(ex.getMessage().contains("не найдено в справочнике"));
    }

    @Test
    void testUpdatePlant() {
        plantService.addDAO(mockDao);
        plantService.updatePlant("Роза", plant1);

        verify(mockDao, times(1)).updatePlant("Роза", plant1);
    }

    @Test
    void testDeletePlant() {
        plantService.addDAO(mockDao);
        plantService.deletePlant("Роза");

        verify(mockDao, times(1)).deletePlant("Роза");
    }

    @Test
    void testPerformWatering() {
        plantService.addDAO(mockDao);
        LocalDate lastWateredBefore = plant1.getLastWatered();
        plantService.performWatering(plant1);

        assertEquals(LocalDate.now(), plant1.getLastWatered());
        assertTrue(plant1.getWateringHistory().contains(lastWateredBefore));
        verify(mockDao, times(1)).updatePlant(anyString(), eq(plant1));
    }

    @Test
    void testCalculateNextWateringForNewPlant() {
        LocalDate nextWatering = plantService.calculateNextWateringForNewPlant(plant1);
        assertNotNull(nextWatering);
        assertTrue(nextWatering.isAfter(plant1.getLastWatered()));
    }

    @Test
    void testUpdateStatus_ActiveAndInactive() {
        Plant activePlant = new Plant("Роза", "декоративно-цветущие", LocalDate.now().minusDays(10), 1.0, new ArrayList<>(), "Активно");
        Plant inactivePlant = new Plant("Кактус", "суккуленты", LocalDate.now().minusDays(31), 0.5, new ArrayList<>(), "Неактивно");

        plantService.addDAO(mockDao);
        plantService.updatePlant("Роза", activePlant);
        plantService.updatePlant("Кактус", inactivePlant);

        plantService.performWatering(activePlant);
        plantService.performWatering(inactivePlant);

        assertEquals("Активно", activePlant.getStatus());
        assertEquals("Активно", inactivePlant.getStatus());
    }

    @Test
    void testGetNextRepotDate() {
        LocalDate repotDate = plantService.getNextRepotDate(plant1);
        assertNotNull(repotDate);
        assertTrue(repotDate.isAfter(plant1.getLastWatered()));
    }

    @Test
    void testDiagnoseDisease_NoSymptoms() {
        Plant healthy = new Plant("Кактус", "суккуленты", LocalDate.now(), 0.5, new ArrayList<>(), "Активно");
        String diagnosis = plantService.diagnoseDisease(healthy);
        assertEquals("Признаков заболеваний не выявлено", diagnosis);
    }

    @Test
    void testDiagnoseDisease_KnownSymptom() {
        Plant sick = new Plant("Роза", "декоративно-цветущие", LocalDate.now(), 1.0, Arrays.asList("желтые листья"), "Активно");
        String diagnosis = plantService.diagnoseDisease(sick);
        assertTrue(diagnosis.contains("Недостаток азота") || diagnosis.contains("старение листьев"));
    }

    @Test
    void testDiagnoseDisease_UnknownSymptom() {
        Plant unknown = new Plant("Роза", "декоративно-цветущие", LocalDate.now(), 1.0, Arrays.asList("странный симптом"), "Активно");
        String diagnosis = plantService.diagnoseDisease(unknown);
        assertTrue(diagnosis.contains("Неопределённый симптом"));
    }

    @Test
    void testIsValidPlantName() {
        assertTrue(plantService.isValidPlantName("Роза"));
        assertFalse(plantService.isValidPlantName("Неизвестное"));
    }

    @Test
    void testIsValidType() {
        assertTrue(plantService.isValidType("декоративно-цветущие"));
        assertFalse(plantService.isValidType("инопланетные"));
    }

    @Test
    void testDetectTypeByName() {
        assertEquals("декоративно-цветущие", plantService.detectTypeByName("Роза"));
        assertNull(plantService.detectTypeByName("Неизвестное"));
    }
}
