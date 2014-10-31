package com.example;

import lombok.Value;
import lombok.experimental.Wither;

/**
 */
@Value
@Wither
public class SampleItem {
    String title;
    String description;
}
