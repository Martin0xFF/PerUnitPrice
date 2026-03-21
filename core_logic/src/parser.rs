use regex::Regex;
use log::{debug, warn};

#[derive(Debug, PartialEq)]
pub enum Unit {
    Grams,
    Litres,
    Item,
}

#[derive(Debug, PartialEq)]
pub struct ParsedInput {
    pub quantity: f64,
    pub unit: Unit,
}

pub fn parse_input(input: &str) -> Option<ParsedInput> {
    let input = input.trim().to_lowercase();
    debug!("Parsing input: '{}'", input);
    
    // If empty or just spaces, it's 1 item
    if input.is_empty() {
        debug!("Input is empty, defaulting to 1 item");
        return Some(ParsedInput { quantity: 1.0, unit: Unit::Item });
    }

    // Match number followed by optional unit
    let re = Regex::new(r"^([\d\.]+)\s*([a-z]*)$").ok()?;
    let caps = re.captures(&input)?;

    let quantity_str = caps.get(1)?.as_str();
    let quantity: f64 = quantity_str.parse().ok()?;

    let unit_str = caps.get(2).map_or("", |m| m.as_str());
    
    let (normalized_quantity, unit) = match unit_str {
        "kg" | "kilogram" | "kilograms" => (quantity * 1000.0, Unit::Grams),
        "g" | "gram" | "grams" => (quantity, Unit::Grams),
        "l" | "liter" | "litre" | "liters" | "litres" => (quantity, Unit::Litres),
        "ml" | "milliliter" | "millilitre" | "milliliters" | "millilitres" => (quantity / 1000.0, Unit::Litres),
        "" | "item" | "items" | "unit" | "units" => (quantity, Unit::Item),
        _ => {
            warn!("Unknown unit: '{}'", unit_str);
            return None;
        }
    };

    debug!("Parsed quantity: {}, unit: {:?}", normalized_quantity, unit);
    Some(ParsedInput { quantity: normalized_quantity, unit })
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
            Some(ParsedInput { quantity: 1500.0, unit: Unit::Grams })
        );
    }

    #[test]
    fn test_parse_ml() {
        assert_eq!(
            parse_input("500ml"),
            Some(ParsedInput { quantity: 0.5, unit: Unit::Litres })
        );
    }

    #[test]
    fn test_parse_empty_unit() {
        assert_eq!(
            parse_input("5"),
            Some(ParsedInput { quantity: 5.0, unit: Unit::Item })
        );
    }

    #[test]
    fn test_calculate_price() {
        assert_eq!(calculate_per_unit_price(10.0, 2.0), 5.0);
    }
}