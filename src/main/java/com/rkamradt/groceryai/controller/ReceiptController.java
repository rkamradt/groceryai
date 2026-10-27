package com.rkamradt.groceryai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rkamradt.groceryai.dto.ExtractedGroceryItem;
import com.rkamradt.groceryai.dto.ExtractedGroceryReceipt;
import com.rkamradt.groceryai.dto.GroceryItemRequest;
import com.rkamradt.groceryai.dto.ItemsRequest;
import com.rkamradt.groceryai.dto.UserInfo;
import com.rkamradt.groceryai.service.AnthropicService;
import com.rkamradt.groceryai.service.ImageProcessingService;
import com.rkamradt.groceryai.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ReceiptController {

    private static final Logger log = LoggerFactory.getLogger(ReceiptController.class);

    private final ImageProcessingService imageService;
    private final AnthropicService anthropicService;
    private final ItemService itemService;
    private final ObjectMapper mapper;

    public ReceiptController(ImageProcessingService imageService,
                             AnthropicService anthropicService,
                             ItemService itemService,
                             ObjectMapper mapper) {
        this.imageService = imageService;
        this.anthropicService = anthropicService;
        this.itemService = itemService;
        this.mapper = mapper;
    }

    /**
     * Returns the signed-in user's profile, or {signedIn:false} if not logged in.
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfo> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.ok(new UserInfo(false, null, null, null));
        }
        if (auth.getPrincipal() instanceof OidcUser user) {
            return ResponseEntity.ok(new UserInfo(true, user.getFullName(),
                user.getEmail(), user.getPicture()));
        }
        return ResponseEntity.ok(new UserInfo(true, auth.getName(), null, null));
    }

    /**
     * Accepts a grocery receipt image, sends to Claude for extraction.
     * Returns the extracted items with prices for user review.
     * Does NOT save anything — that happens on /api/receipt/confirm.
     */
    @PostMapping("/receipt/extract")
    public ResponseEntity<ExtractedGroceryReceipt> extract(
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws Exception {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("receipt/extract: file={} size={} user={}",
            file.getOriginalFilename(), file.getSize(), auth.getName());

        byte[] processed = imageService.process(file.getBytes(), file.getContentType());
        ExtractedGroceryReceipt raw = anthropicService.extract(processed);
        var enriched = anthropicService.enrichItems(raw.items(), raw.storeName());
        return ResponseEntity.ok(new ExtractedGroceryReceipt(
            raw.storeName(), raw.date(), enriched, raw.total(), raw.confidenceNotes()));
    }

    /**
     * Confirms the user-reviewed extracted items: runs USDA lookup per item,
     * saves everything to the week, and stores the receipt total.
     *
     * Expects a multipart form with:
     *   weekId  — UUID of the week
     *   data    — JSON: list of {name, category, servingSizeG, servingsPerWeek, price}
     *   total   — receipt grand total (optional)
     */
    @PostMapping("/receipt/confirm")
    public ResponseEntity<Void> confirm(
            @RequestParam("weekId") UUID weekId,
            @RequestParam("data") String dataJson,
            @RequestParam(value = "total", required = false) Double total,
            Authentication auth) throws Exception {

        List<GroceryItemRequest> items = mapper.readValue(dataJson,
            mapper.getTypeFactory().constructCollectionType(List.class, GroceryItemRequest.class));

        log.info("receipt/confirm: weekId={} items={} total={} user={}",
            weekId, items.size(), total, auth.getName());

        itemService.saveItems(new ItemsRequest(weekId, items, total));
        return ResponseEntity.ok().build();
    }
}
