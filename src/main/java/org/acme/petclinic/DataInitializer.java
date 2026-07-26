package org.acme.petclinic;

import org.acme.petclinic.entities.*;
import org.acme.petclinic.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PetTypeRepository petTypeRepository;

    @Autowired
    private VetRepository vetRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Override
    public void run(String... args) throws Exception {
        // Only initialize if database is empty
        if (ownerRepository.count() == 0) {
            initializeData();
        }
    }

    private void initializeData() {
        System.out.println("🌱 Initializing database with dummy data...");

        // Create Pet Types
        PetType dog = new PetType(null, "Dog");
        PetType cat = new PetType(null, "Cat");
        PetType bird = new PetType(null, "Bird");
        PetType rabbit = new PetType(null, "Rabbit");
        PetType hamster = new PetType(null, "Hamster");

        petTypeRepository.saveAll(Arrays.asList(dog, cat, bird, rabbit, hamster));
        System.out.println("  → Created pet types");

        // Create Owners
        List<Owner> owners = Arrays.asList(
            new Owner(null, "George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023"),
            new Owner(null, "Betty", "Davis", "638 Morris Ave.", "Madison", "6085551749"),
            new Owner(null, "Eduardo", "Rodriquez", "2693 Commerce Ave.", "McFarland", "6085559435"),
            new Owner(null, "Harold", "Davis", "563 Friendly St.", "Windsor", "6085553198"),
            new Owner(null, "Peter", "McTavish", "2387 S. Fair Way", "Madison", "6085552765"),
            new Owner(null, "Jean", "Coleman", "105 N. Lake St.", "Monona", "6085552654"),
            new Owner(null, "Jeff", "Black", "1450 Oak Blvd.", "Monona", "6085555387"),
            new Owner(null, "Maria", "Escobito", "345 Maple Dr.", "Madison", "6085555521"),
            new Owner(null, "David", "Schroeder", "2749 Blackhawk Trail", "Madison", "6085559435"),
            new Owner(null, "Carlos", "Estevez", "2335 Independence La.", "Waunakee", "6085555487")
        );
        ownerRepository.saveAll(owners);
        System.out.println("  → Created 10 owners");

        // Create Pets
        List<Pet> pets = Arrays.asList(
            new Pet(null, "Leo", LocalDate.of(2020, 9, 7), dog, owners.get(0)),
            new Pet(null, "Basil", LocalDate.of(2021, 8, 6), cat, owners.get(1)),
            new Pet(null, "Rosy", LocalDate.of(2019, 4, 17), dog, owners.get(2)),
            new Pet(null, "Jewel", LocalDate.of(2020, 3, 7), cat, owners.get(3)),
            new Pet(null, "Lucky", LocalDate.of(2020, 6, 24), dog, owners.get(4)),
            new Pet(null, "Mulligan", LocalDate.of(2021, 9, 4), hamster, owners.get(5)),
            new Pet(null, "Freddy", LocalDate.of(2020, 3, 9), cat, owners.get(6)),
            new Pet(null, "Lucky", LocalDate.of(2022, 6, 24), dog, owners.get(7)),
            new Pet(null, "Sly", LocalDate.of(2020, 8, 6), cat, owners.get(8)),
            new Pet(null, "Pullo", LocalDate.of(2021, 5, 12), rabbit, owners.get(9)),
            new Pet(null, "Birdie", LocalDate.of(2020, 11, 3), bird, owners.get(0)),
            new Pet(null, "Tweety", LocalDate.of(2021, 1, 15), bird, owners.get(1))
        );
        petRepository.saveAll(pets);
        System.out.println("  → Created 12 pets");

        // Create Vets
        List<Vet> vets = Arrays.asList(
            new Vet(null, "James", "Carter", "Radiology"),
            new Vet(null, "Helen", "Leary", "Surgery"),
            new Vet(null, "Linda", "Douglas", "Emergency"),
            new Vet(null, "Rafael", "Ortega", "Dentistry"),
            new Vet(null, "Henry", "Stevens", "General"),
            new Vet(null, "Sharon", "Jenkins", "Ophthalmology")
        );
        vetRepository.saveAll(vets);
        System.out.println("  → Created 6 veterinarians");

        // Create Visits
        List<Visit> visits = Arrays.asList(
            new Visit(null, LocalDate.of(2023, 1, 1), "rabies shot", pets.get(0), vets.get(0)),
            new Visit(null, LocalDate.of(2023, 3, 4), "neutered", pets.get(2), vets.get(1)),
            new Visit(null, LocalDate.of(2023, 6, 4), "dental checkup", pets.get(1), vets.get(3)),
            new Visit(null, LocalDate.of(2023, 8, 4), "physical exam", pets.get(3), vets.get(4)),
            new Visit(null, LocalDate.of(2023, 10, 20), "vaccinations", pets.get(4), vets.get(0)),
            new Visit(null, LocalDate.of(2023, 12, 1), "eye exam", pets.get(5), vets.get(5)),
            new Visit(null, LocalDate.of(2024, 1, 14), "check-up", pets.get(6), vets.get(4)),
            new Visit(null, LocalDate.of(2024, 2, 15), "surgery consult", pets.get(7), vets.get(1)),
            new Visit(null, LocalDate.of(2024, 3, 3), "wellness visit", pets.get(8), vets.get(4)),
            new Visit(null, LocalDate.of(2024, 4, 6), "checkup", pets.get(9), vets.get(4)),
            new Visit(null, LocalDate.of(2024, 5, 12), "vaccinations", pets.get(10), vets.get(0)),
            new Visit(null, LocalDate.of(2024, 6, 3), "follow-up", pets.get(11), vets.get(2))
        );
        visitRepository.saveAll(visits);
        System.out.println("  → Created 12 visits");

        System.out.println("✅ Database initialization complete!");
        System.out.println("   • 10 owners created");
        System.out.println("   • 12 pets created");
        System.out.println("   • 6 veterinarians created");
        System.out.println("   • 12 visits created");
        System.out.println("   • 5 pet types created");
    }
}
