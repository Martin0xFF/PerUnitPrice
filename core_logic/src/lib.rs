mod parser;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jstring, JNI_VERSION_1_6};
use log::{debug, error, info};
use android_logger::Config;

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_vm: *mut std::ffi::c_void, _reserved: *mut std::ffi::c_void) -> jint {
    android_logger::init_once(
        Config::default().with_tag("[PUP][Core]"),
    );
    info!("Core logic library loaded and logger initialized");
    JNI_VERSION_1_6
}

#[no_mangle]
pub extern "system" fn Java_com_zeroff_perunitprice_MainActivity_calculatePerUnitPrice(
    mut env: JNIEnv,
    _class: JClass,
    price: f64,
    quantity_str: JString,
) -> jstring {
    let input: String = env
        .get_string(&quantity_str)
        .expect("Couldn't get java string!")
        .into();

    debug!("calculatePerUnitPrice called with price: {}, quantity_str: '{}'", price, input);

    let result = match parser::parse_input(&input) {
        Some(parsed) => {
            let per_unit = parser::calculate_per_unit_price(price, parsed.quantity);
            let unit_name = match parsed.unit {
                parser::Unit::Grams => "g",
                parser::Unit::Litres => "L",
                parser::Unit::Item => "item",
            };
            let formatted = format!("{:.2} / {}", per_unit, unit_name);
            info!("Successfully parsed input. Per unit price: {}", formatted);
            formatted
        }
        None => {
            error!("Failed to parse input: '{}'", input);
            "Invalid input format".to_string()
        }
    };

    let output = env.new_string(result).expect("Couldn't create java string!");
    output.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_zeroff_perunitprice_MainActivity_calculateRawPerUnitPrice(
    mut env: JNIEnv,
    _class: JClass,
    price: f64,
    quantity_str: JString,
) -> jni::sys::jdouble {
    let input: String = env
        .get_string(&quantity_str)
        .expect("Couldn't get java string!")
        .into();

    match parser::parse_input(&input) {
        Some(parsed) => {
            parser::calculate_per_unit_price(price, parsed.quantity)
        }
        None => {
            f64::MAX // Return a high value to sort invalid ones to the bottom
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_zeroff_perunitprice_MainActivity_getUnit(
    mut env: JNIEnv,
    _class: JClass,
    quantity_str: JString,
) -> jstring {
    let input: String = env
        .get_string(&quantity_str)
        .expect("Couldn't get java string!")
        .into();

    let unit = parser::get_unit_str(&input);
    let output = env.new_string(unit).expect("Couldn't create java string!");
    output.into_raw()
}