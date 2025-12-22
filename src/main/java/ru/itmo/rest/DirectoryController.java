package ru.itmo.rest;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.itmo.rest.dto.DragonResponseDTO;
import ru.itmo.rest.dto.PaginatedDragonsResponse;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/killer")
public class DirectoryController {


    private final WebClient serviceAClient;

    public DirectoryController(@Qualifier("serviceAClient") WebClient serviceAClient) {
        this.serviceAClient = serviceAClient;
    }

    @GetMapping("/")
    public String test(){
        return "Hello";
    }

    @GetMapping("/dragon/find-by-cave-depth/{max}")
    public Mono<DragonResponseDTO> findByCaveDepth(@PathVariable boolean max) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("page", 1);
        requestBody.put("size", 100);

        List<Map<String, String>> sortList = new ArrayList<>();
        Map<String, String> sort = new HashMap<>();
        sort.put("by", "coordinates.x");
        sort.put("order", max ? "desc" : "asc");
        sortList.add(sort);
        requestBody.put("sort", sortList);

        return serviceAClient.post()
                .uri("/dragons/search")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(PaginatedDragonsResponse.class)
                .map(response -> {
                    if (response.getItems() != null && !response.getItems().isEmpty()) {
                        List<DragonResponseDTO> dragons = response.getItems();

                        List<DragonResponseDTO> dragonsWithCoordinates = dragons.stream()
                                .filter(d -> d.getCoordinates() != null && d.getCoordinates().getX() != null)
                                .collect(Collectors.toList());

                        if (dragonsWithCoordinates.isEmpty()) {
                            throw new RuntimeException("Драконы с координатами не найдены");
                        }

                        dragonsWithCoordinates.sort((d1, d2) -> {
                            Long x1 = d1.getCoordinates().getX();
                            Long x2 = d2.getCoordinates().getX();
                            return max ? Long.compare(x2, x1) : Long.compare(x1, x2);
                        });

                        return dragonsWithCoordinates.get(0);
                    } else {
                        throw new RuntimeException("Драконы не найдены");
                    }
                });
    }

    @PostMapping("/team/{teamId}/move-to-cave/{caveId}")
    public Mono<Map<String, Object>> moveTeamToCave(
            @PathVariable String teamId,
            @PathVariable Double caveId) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("page", 1);
        requestBody.put("size", 100);

        Map<String, Object> filters = new HashMap<>();
        filters.put("coordinatesY", caveId);
        requestBody.put("filters", filters);

        return serviceAClient.post()
                .uri("/dragons/search")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(PaginatedDragonsResponse.class)
                .flatMap(response -> {
                    List<DragonResponseDTO> dragons = response.getItems();

                    if (dragons == null) {
                        dragons = Collections.emptyList();
                    }

                    List<DragonResponseDTO> filteredDragons = dragons.stream()
                            .filter(d -> d.getCoordinates() != null &&
                                    d.getCoordinates().getY() != null &&
                                    d.getCoordinates().getY().equals(caveId))
                            .collect(Collectors.toList());

                    if (filteredDragons.isEmpty()) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("teamId", teamId);
                        result.put("caveId", caveId);
                        result.put("success", false);
                        result.put("message", "Пещера пуста - драконов не обнаружено");
                        return Mono.just(result);
                    }

                    DragonResponseDTO dragon = filteredDragons.get(0);
                    Long dragonId = dragon.getId();

                    return serviceAClient.delete()
                            .uri("/dragons/{id}", dragonId)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .then(Mono.fromCallable(() -> {
                                Map<String, Object> result = new HashMap<>();
                                result.put("teamId", teamId);
                                result.put("caveId", caveId);
                                result.put("success", true);
                                result.put("killedDragonId", dragonId);
                                result.put("killedDragonName", dragon.getName());
                                result.put("message",
                                        String.format("Команда '%s' успешно уничтожила дракона '%s' в пещере %.0f!",
                                                teamId, dragon.getName(), caveId));
                                return result;
                            }));
                })
                .onErrorResume(throwable -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("teamId", teamId);
                    result.put("caveId", caveId);
                    result.put("success", false);
                    result.put("error", throwable.getMessage());
                    result.put("message", "Миссия провалилась из-за технической ошибки");
                    return Mono.just(result);
                });
    }
    }