package org.acme.petclinic.resources;

import org.acme.petclinic.entities.Owner;
import org.acme.petclinic.repositories.OwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/owners")
public class OwnerController {

    @Autowired
    private OwnerRepository ownerRepository;

    @GetMapping
    public List<Owner> listAll() {
        return ownerRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Owner> get(@PathVariable Long id) {
        Optional<Owner> owner = ownerRepository.findById(id);
        return owner.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Owner create(@RequestBody Owner owner) {
        return ownerRepository.save(owner);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Owner> update(@PathVariable Long id, @RequestBody Owner ownerDetails) {
        Optional<Owner> owner = ownerRepository.findById(id);
        if (owner.isPresent()) {
            Owner o = owner.get();
            o.setFirstName(ownerDetails.getFirstName());
            o.setLastName(ownerDetails.getLastName());
            o.setAddress(ownerDetails.getAddress());
            o.setCity(ownerDetails.getCity());
            o.setTelephone(ownerDetails.getTelephone());
            return ResponseEntity.ok(ownerRepository.save(o));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (ownerRepository.existsById(id)) {
            ownerRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
