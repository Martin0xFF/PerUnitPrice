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
pub extern "system" fn Java_com_zeroff_perunitprice_MainActivity_addProduct(
    mut env: JNIEnv,
    _class: JClass,
    name: JString,
    price_input: JString,
    quantity_input: JString,
) {
    let name: String = env.get_string(&name).unwrap().into();
    let price_input: String = env.get_string(&price_input).unwrap().into();
    let quantity_input: String = env.get_string(&quantity_input).unwrap().into();

    parser::add_product(name, price_input, quantity_input);
}

#[no_mangle]
pub extern "system" fn Java_com_zeroff_perunitprice_MainActivity_clearProducts(
    _env: JNIEnv,
    _class: JClass,
) {
    parser::clear_products();
}

#[no_mangle]
pub extern "system" fn Java_com_zeroff_perunitprice_MainActivity_getProductCount(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    parser::get_product_count() as jint
}

#[no_mangle]
pub extern "system" fn Java_com_zeroff_perunitprice_MainActivity_getProductAt(
    mut env: JNIEnv,
    _class: JClass,
    index: jint,
) -> jstring {
    if let Some(product) = parser::get_product_at(index as usize) {
        // Return a semicolon-separated string for simplicity
        let result = format!("{};{};{};{};{}", 
            product.name, 
            product.price_input, 
            product.quantity_input, 
            product.formatted_result, 
            product.raw_per_unit_price
        );
        let output = env.new_string(result).expect("Couldn't create java string!");
        output.into_raw()
    } else {
        let output = env.new_string("").expect("Couldn't create java string!");
        output.into_raw()
    }
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
            let unit_name = if parsed.unit.is_empty() { "unit" } else { &parsed.unit };
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