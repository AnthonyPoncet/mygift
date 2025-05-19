use crate::configuration::Configuration;
use crate::managers::wishlist_manager::WishList;
use crate::routes::files::resize_file;
use printpdf::{
    Actions, BorderArray, Color, ColorArray, FontId, HighlightingMode, LinePoint, LinkAnnotation,
    Mm, Op, PaintMode, PdfDocument, PdfPage, PdfSaveOptions, Point, Polygon, PolygonRing, Pt,
    RawImage, Rect, Rgb, Svg, TextAlign, TextShapingOptions, WindingOrder, XObjectTransform,
};
use std::path::PathBuf;
use std::sync::Arc;

const PAGE_WIDTH: Mm = Mm(525.0);
const PAGE_HEIGHT: Mm = Mm(286.0);
const GIFT_SIZE_PXL: usize = 880; //Pixels
const GIFT_SIZE_MM: Mm = Mm(74.5);
const PRICE_TAG_DIAMETER: Mm = Mm(20.0);
const POSITIONS: [(Mm, Mm); 5] = [
    (Mm(25.0), Mm(150.0)),
    (Mm(125.0), Mm(150.0)),
    (Mm(225.0), Mm(150.0)),
    (Mm(325.0), Mm(150.0)),
    (Mm(425.0), Mm(150.0)),
];

pub fn get_pdf(wishlist: WishList, configuration: Arc<Configuration>) -> Vec<u8> {
    let mut doc = PdfDocument::new("Wishlist");

    let roboto_regular_font_id = get_font(
        include_bytes!("../../resource/Roboto-Regular.ttf"),
        &mut doc,
    );
    let roboto_bold_font_id = get_font(include_bytes!("../../resource/Roboto-Bold.ttf"), &mut doc);

    let category_name_options = TextShapingOptions {
        font_size: Pt(36.0),
        max_width: Some(PAGE_WIDTH.into_pt()),
        align: TextAlign::Center,
        ..Default::default()
    };

    let gift_name_options = TextShapingOptions {
        font_size: Pt(18.0),
        max_width: Some(GIFT_SIZE_MM.into_pt()),
        align: TextAlign::Center,
        ..Default::default()
    };

    let gift_description_options = TextShapingOptions {
        font_size: Pt(12.0),
        max_width: Some(GIFT_SIZE_MM.into_pt()),
        align: TextAlign::Center,
        ..Default::default()
    };

    let gift_price_options = TextShapingOptions {
        font_size: Pt(14.0),
        max_width: Some(PRICE_TAG_DIAMETER.into_pt()),
        align: TextAlign::Center,
        ..Default::default()
    };

    let heart = Svg::parse(include_str!("../../resource/heart.svg"), &mut Vec::new()).unwrap();
    let heart_id = doc.add_xobject(&heart);

    let content = include_bytes!("../../front-vue/src/assets/images/blank_profile_picture.png");
    let blank_gift = RawImage::decode_from_bytes(content, &mut Vec::new()).unwrap();

    let mut pages = Vec::new();
    for category in wishlist.categories {
        let mut ops = Vec::new();
        for (index, gift) in category.gifts.into_iter().enumerate() {
            if index % 5 == 0 {
                if !ops.is_empty() {
                    let page = PdfPage::new(PAGE_WIDTH, PAGE_HEIGHT, ops);
                    ops = Vec::new();
                    pages.push(page);
                }

                let shaped_text = doc
                    .shape_text(&category.name, &roboto_bold_font_id, &category_name_options)
                    .unwrap();
                let origin = Point {
                    x: Pt(0.0),
                    y: (PAGE_HEIGHT - Mm(10.0)).into_pt(),
                };
                ops.extend(shaped_text.get_ops(origin));
            }

            let image = if let Some(picture) = gift.picture {
                let root_path = PathBuf::from(&configuration.upload_file_storage);
                let root_output_path = root_path.parent().unwrap().to_path_buf().join("tmp_rs");
                let original_file = root_path.join(picture);
                if let Some(output_file) = resize_file(&original_file, &root_output_path) {
                    let content = std::fs::read(output_file).unwrap();
                    RawImage::decode_from_bytes(&content, &mut Vec::new()).unwrap()
                } else {
                    blank_gift.clone()
                }
            } else {
                blank_gift.clone()
            };

            let scale_height = GIFT_SIZE_PXL as f32 / image.height as f32;
            let scale_width = GIFT_SIZE_PXL as f32 / image.width as f32;
            let scale = if scale_height > scale_width {
                scale_width
            } else {
                scale_height
            };
            let add_x = ((GIFT_SIZE_PXL as f32 - image.width as f32 * scale) / 2.0)
                * (GIFT_SIZE_MM.0 / GIFT_SIZE_PXL as f32);
            let add_y = ((GIFT_SIZE_PXL as f32 - image.height as f32 * scale) / 2.0)
                * (GIFT_SIZE_MM.0 / GIFT_SIZE_PXL as f32);

            let (x, y) = POSITIONS[index % 5];
            ops.push(Op::UseXobject {
                id: doc.add_image(&image),
                transform: XObjectTransform {
                    translate_x: Some((x + Mm(add_x)).into_pt()),
                    translate_y: Some((y + Mm(add_y)).into_pt()),
                    scale_x: Some(scale),
                    scale_y: Some(scale),
                    ..Default::default()
                },
            });
            ops.push(Op::DrawPolygon {
                polygon: Polygon {
                    rings: vec![PolygonRing {
                        points: vec![
                            LinePoint {
                                p: Point {
                                    x: (x - Mm(5.0)).into_pt(),
                                    y: (y - Mm(120.0)).into_pt(),
                                },
                                bezier: false,
                            },
                            LinePoint {
                                p: Point {
                                    x: (x + Mm(80.0)).into_pt(),
                                    y: (y - Mm(120.0)).into_pt(),
                                },
                                bezier: false,
                            },
                            LinePoint {
                                p: Point {
                                    x: (x + Mm(80.0)).into_pt(),
                                    y: (y + Mm(80.0)).into_pt(),
                                },
                                bezier: false,
                            },
                            LinePoint {
                                p: Point {
                                    x: (x - Mm(5.0)).into_pt(),
                                    y: (y + Mm(80.0)).into_pt(),
                                },
                                bezier: false,
                            },
                        ],
                    }],
                    mode: PaintMode::Stroke,
                    winding_order: WindingOrder::NonZero,
                },
            });

            let shaped_text = doc
                .shape_text(&gift.name, &roboto_regular_font_id, &gift_name_options)
                .unwrap();
            let origin = Point {
                x: x.into_pt(),
                y: (y - Mm(10.0)).into_pt(),
            };
            ops.extend(shaped_text.get_ops(origin));

            if let Some(description) = gift.description {
                let shaped_text = doc
                    .shape_text(
                        &description,
                        &roboto_regular_font_id,
                        &gift_description_options,
                    )
                    .unwrap();
                let origin = Point {
                    x: x.into_pt(),
                    y: (y - Mm(30.0)).into_pt(),
                };
                ops.extend(shaped_text.get_ops(origin));
            }

            if let Some(price) = gift.price {
                let diameter = Mm(20.0);
                let radius = (diameter / 2.0).into_pt();
                let offset_x = (x + Mm(79.0)).into_pt() - radius;
                let offset_y = (y + Mm(79.0)).into_pt() - radius;
                let k = 0.551_915_05;
                let k_radius = Pt(k * radius.0);

                ops.push(Op::SaveGraphicsState);
                ops.push(Op::SetFillColor {
                    col: Color::Rgb(Rgb {
                        r: 0.86,
                        g: 0.86,
                        b: 0.86,
                        icc_profile: None,
                    }),
                });
                ops.push(Op::DrawPolygon {
                    polygon: Polygon {
                        rings: vec![PolygonRing {
                            points: vec![
                                LinePoint {
                                    p: Point {
                                        x: offset_x,
                                        y: offset_y + radius,
                                    },
                                    bezier: true,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x + k_radius,
                                        y: offset_y + radius,
                                    },
                                    bezier: true,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x + radius,
                                        y: offset_y + k_radius,
                                    },
                                    bezier: true,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x + radius,
                                        y: offset_y,
                                    },
                                    bezier: false,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x + radius,
                                        y: offset_y - k_radius,
                                    },
                                    bezier: true,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x + k_radius,
                                        y: offset_y - radius,
                                    },
                                    bezier: true,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x,
                                        y: offset_y - radius,
                                    },
                                    bezier: false,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x - k_radius,
                                        y: offset_y - radius,
                                    },
                                    bezier: true,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x - radius,
                                        y: offset_y - k_radius,
                                    },
                                    bezier: true,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x - radius,
                                        y: offset_y,
                                    },
                                    bezier: false,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x - radius,
                                        y: offset_y + k_radius,
                                    },
                                    bezier: true,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x - k_radius,
                                        y: offset_y + radius,
                                    },
                                    bezier: true,
                                },
                                LinePoint {
                                    p: Point {
                                        x: offset_x,
                                        y: offset_y + radius,
                                    },
                                    bezier: false,
                                },
                            ],
                        }],
                        mode: PaintMode::Fill,
                        winding_order: WindingOrder::NonZero,
                    },
                });
                ops.push(Op::RestoreGraphicsState);

                let shaped_text = doc
                    .shape_text(&price, &roboto_regular_font_id, &gift_price_options)
                    .unwrap();
                let origin = Point {
                    x: offset_x - radius,
                    y: offset_y + Mm(3.5).into_pt(),
                };
                ops.extend(shaped_text.get_ops(origin));
            }

            if let Some(where_to_buy) = gift.where_to_buy {
                ops.push(Op::LinkAnnotation {
                    link: LinkAnnotation {
                        rect: Rect {
                            x: x.into_pt(),
                            y: y.into_pt(),
                            width: GIFT_SIZE_MM.into_pt(),
                            height: GIFT_SIZE_MM.into_pt(),
                        },
                        actions: Actions::Uri(where_to_buy),
                        border: BorderArray::default(),
                        color: ColorArray::default(),
                        highlighting: HighlightingMode::None,
                    },
                })
            }

            if gift.heart {
                ops.push(Op::UseXobject {
                    id: heart_id.clone(),
                    transform: XObjectTransform {
                        translate_x: Some((x - Mm(5.0)).into_pt()),
                        translate_y: Some((y + Mm(64.0)).into_pt()),
                        scale_x: Some(0.25),
                        scale_y: Some(0.25),
                        ..Default::default()
                    },
                });
            }
        }

        if !ops.is_empty() {
            let page = PdfPage::new(PAGE_WIDTH, PAGE_HEIGHT, ops);
            pages.push(page);
        }
    }

    doc.with_pages(pages)
        .save(&PdfSaveOptions::default(), &mut Vec::new())
}

fn get_font(font_bytes: &[u8], doc: &mut PdfDocument) -> FontId {
    let font = printpdf::ParsedFont::from_bytes(font_bytes, 0, &mut Vec::new()).unwrap();
    doc.add_font(&font)
}
