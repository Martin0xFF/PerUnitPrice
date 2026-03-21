mod parser;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

#[no_mangle]
pub extern "system" fn Java_com_example_perunitprice_MainActivity_calculatePerUnitPrice(
    mut env: JNIEnv,
    _class: JClass,
    price: f64,
    quantity_str: JString,
) -> jstring {
    let input: String = env
        .get_string(&quantity_str)
        .expect("Couldn't get java string!")
        .into();

    let result = match parser::parse_input(&input) {
        Some(parsed) => {
            let per_unit = parser::calculate_per_unit_price(price, parsed.quantity);
            let unit_name = match parsed.unit {
                parser::Unit::Grams => "g",
                parser::Unit::Litres => "L",
                parser::Unit::Item => "item",
            };
            format!("{:.2} / {}", per_unit, unit_name)
        }
        None => "Invalid input format".to_string(),
    };

    let output = env.new_string(result).expect("Couldn't create java string!");
    output.into_raw()
}