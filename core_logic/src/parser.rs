use regex::Regex;
use log::{debug, warn, info};
use std::collections::BinaryHeap;
use std::cmp::Reverse;
use std::sync::Mutex;
use once_cell::sync::Lazy;

#[derive(Debug, Clone, PartialEq)]
pub struct Product {
    pub name: String,
    pub price_input: String,
    pub quantity_input: String,
    pub formatted_result: String,
    pub raw_per_unit_price: f64,
}

impl Eq for Product {}

impl PartialOrd for Product {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        self.raw_per_unit_price.partial_cmp(&other.raw_per_unit_price)
    }
}

impl Ord for Product {
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        self.partial_cmp(other).unwrap_or(std::cmp::Ordering::Equal)
    }
}

pub struct ProductStore {
    heap: BinaryHeap<Reverse<Product>>,
    cached_sorted: Vec<Product>,
}

static STORE: Lazy<Mutex<ProductStore>> = Lazy::new(|| {
    Mutex::new(ProductStore {
        heap: BinaryHeap::new(),
        cached_sorted: Vec::new(),
    })
});

pub fn add_product(name: String, price_input: String, quantity_input: String) {
    let price: f64 = price_input.parse().unwrap_or(0.0);
    let (formatted_result, raw_per_unit_price) = match parse_input(&quantity_input) {
        Some(parsed) => {
            let per_unit = calculate_per_unit_price(price, parsed.quantity);
            let unit_name = if parsed.unit.is_empty() { "unit" } else { &parsed.unit };
            (format!("{:.2} / {}", per_unit, unit_name), per_unit)
        }
        None => ("Invalid input".to_string(), f64::MAX)
    };

    let product = Product {
        name,
        price_input,
        quantity_input,
        formatted_result,
        raw_per_unit_price,
    };

    let mut store = STORE.lock().unwrap();
    store.heap.push(Reverse(product));
    
    // Update cache
    let mut temp_heap = store.heap.clone();
    store.cached_sorted.clear();
    while let Some(Reverse(p)) = temp_heap.pop() {
        store.cached_sorted.push(p);
    }
    info!("Product added. Total products: {}", store.cached_sorted.len());
}

pub fn clear_products() {
    let mut store = STORE.lock().unwrap();
    store.heap.clear();
    store.cached_sorted.clear();
    info!("Product store cleared");
}

pub fn get_product_count() -> usize {
    STORE.lock().unwrap().cached_sorted.len()
}

pub fn get_product_at(index: usize) -> Option<Product> {
    STORE.lock().unwrap().cached_sorted.get(index).cloned()
}

#[derive(Debug, PartialEq)]
pub struct ParsedInput {
    pub quantity: f64,
    pub unit: String,
}

pub fn parse_input(input: &str) -> Option<ParsedInput> {
    let input = input.trim().to_lowercase();
    debug!("Parsing input: '{}'", input);
    
    if input.is_empty() {
        debug!("Input is empty, defaulting to 1 unit");
        return Some(ParsedInput { quantity: 1.0, unit: "".to_string() });
    }

    // Match number followed by optional unit string
    let re = Regex::new(r"^([\d\.]+)\s*(.*)$").ok()?;
    let caps = re.captures(&input)?;

    let quantity_str = caps.get(1)?.as_str();
    let quantity: f64 = quantity_str.parse().ok()?;
    let unit = caps.get(2).map_or("", |m| m.as_str()).trim().to_string();

    debug!("Parsed quantity: {}, unit: '{}'", quantity, unit);
    Some(ParsedInput { quantity, unit })
}

pub fn get_unit_str(input: &str) -> String {
    parse_input(input).map(|p| p.unit).unwrap_or_else(|| "".to_string())
}

pub fn calculate_per_unit_price(price: f64, quantity: f64) -> f64 {
    if quantity <= 0.0 {
        warn!("Quantity is <= 0: {}", quantity);
        return 0.0;
    }
    price / quantity
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_kg() {
        assert_eq!(
            parse_input("1.5 kg"),
            Some(ParsedInput { quantity: 1.5, unit: "kg".to_string() })
        );
    }

    #[test]
    fn test_parse_ml() {
        assert_eq!(
            parse_input("500ml"),
            Some(ParsedInput { quantity: 500.0, unit: "ml".to_string() })
        );
    }

    #[test]
    fn test_parse_empty_unit() {
        assert_eq!(
            parse_input("5"),
            Some(ParsedInput { quantity: 5.0, unit: "".to_string() })
        );
    }

    #[test]
    fn test_calculate_price() {
        assert_eq!(calculate_per_unit_price(10.0, 2.0), 5.0);
    }

    #[test]
    fn test_get_unit_str() {
        assert_eq!(get_unit_str("1.5 kg"), "kg");
        assert_eq!(get_unit_str("500g"), "g");
        assert_eq!(get_unit_str("10"), "");
        assert_eq!(get_unit_str("  2.0  Litres  "), "litres");
    }

    #[test]
    fn test_product_store_sorting() {
        clear_products();
        add_product("Expensive".to_string(), "100.0".to_string(), "1".to_string());
        add_product("Cheap".to_string(), "10.0".to_string(), "1".to_string());
        add_product("Medium".to_string(), "50.0".to_string(), "1".to_string());

        assert_eq!(get_product_count(), 3);
        assert_eq!(get_product_at(0).unwrap().name, "Cheap");
        assert_eq!(get_product_at(1).unwrap().name, "Medium");
        assert_eq!(get_product_at(2).unwrap().name, "Expensive");
    }
}