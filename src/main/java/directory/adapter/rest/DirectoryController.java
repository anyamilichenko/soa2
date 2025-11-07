package directory.adapter.rest;

import directory.adapter.rest.dto.*;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/killer")
public class DirectoryController {

    private final WebClient serviceAClient;

    public DirectoryController(WebClient serviceAClient) {
        this.serviceAClient = serviceAClient;
    }

    @GetMapping("/dragon/find-by-cave-depth/{max}")
    public Mono<DragonResponseDTO> findByCaveDepth(@PathVariable boolean max) {

        String sortParam = max ? "coordinates.x:desc" : "coordinates.x:asc";

        return serviceAClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dragons")
                        .queryParam("page", 1)
                        .queryParam("size", 1)
                        .queryParam("sort", sortParam)
                        .build())
                .retrieve()
                .bodyToMono(PaginatedDragonsResponse.class)
                .map(response -> {
                    if (response.getItems() != null && !response.getItems().isEmpty()) {
                        return response.getItems().get(0);
                    } else {
                        throw new RuntimeException("Драконы не найдены");
                    }
                });
    }

    @PostMapping("/team/{teamId}/move-to-cave/{caveId}")
    public Mono<Map<String, Object>> moveTeamToCave(
            @PathVariable String teamId,
            @PathVariable Double caveId) {

        // предполагаем, что координата Y это "ID пещеры"
        return serviceAClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dragons")
                        .queryParam("coordinates.y", caveId)
                        .queryParam("size", 1)
                        .build())
                .retrieve()
                .bodyToMono(PaginatedDragonsResponse.class)
                .flatMap(response -> {

                    if (response.getItems() == null || response.getItems().isEmpty()) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("teamId", teamId);
                        result.put("caveId", caveId);
                        result.put("success", false);
                        result.put("message", "Пещера пуста - драконов не обнаружено");
                        return Mono.just(result);
                    }

                    DragonResponseDTO dragon = response.getItems().get(0);
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
                                        String.format("Команда '%s' успешно уничтожила дракона '%s' в пещере %d!",
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